package com.leiju.shoot;
import com.leiju.shoot.R;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.util.AttributeSet;
import android.view.View;
import android.content.res.TypedArray;
import android.text.TextPaint;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

enum MyState {
  run,
  pause,
  dead
}

enum ClickType {
  unknown,
  move,
  click,
  dclick
}

public class F16View extends View {
  // constants
  private final int g_life = 3;
  private final int g_bk_color = 0xFFD6DCDF;
  private final int g_txt_color = 0xFF383838;
  private final int g_score_font = 18;
  private final int g_dlg_font = 22;
  private final int g_level_score = 100;
  
  // self
  private int m_life = g_life;   // surive N hit
  private F16 m_f16 = null;
  private MyState m_state = MyState.dead; // ori state
  private int m_record = 0;
  private int m_score = 0;

  private int m_level = 1;   // game difficulty level

  // roles
  private List<AnimateBase> m_ani_list = new ArrayList<AnimateBase>();
  private List<AnimateBase> m_ani_list_cache = new ArrayList<AnimateBase>();

  // painter
  private Paint m_painter;
  private Paint m_txt_painter;

  private float m_density = getResources().getDisplayMetrics().density;
  private  long m_frame_id = 0;

  private float m_score_font = g_score_font;
  private float m_dlg_font = g_dlg_font;

  // btn
  private Rect m_btn_rect = new Rect();

  // click evts
  // XXX remove XXX
  private static final int g_click_lapse = 200;  // ms
  private static final int g_dclick_lapse = 400; // ms
  private float m_click_pos_x = -1;
  private float m_click_pos_y = -1;
  private  long m_ts_down = -1;
  private  long m_ts_up = -1;
  // private  long m_ts_click = -1;
  private  long m_ts_big_hitted = -1;

  // screen density
  public  float screen_den() { return m_density; }
  // score
  public void score(int _value) {
    m_score += _value;

    // next level
    if (m_level < m_score/g_level_score)
      m_level++;

    // the time a big got hitted
    if (_value == 30)
      m_ts_big_hitted = System.currentTimeMillis();
  }

  // constructors
  public F16View(Context _ctx) {
    super(_ctx);
    init(null, 0);
  }
  public F16View(Context _ctx, AttributeSet _attrs) {
    super(_ctx, _attrs);
    init(_attrs, 0);
  }
  public F16View(Context _ctx, AttributeSet _attrs, int _style) {
    super(_ctx, _attrs, _style);
    init(_attrs, _style);
  }

  private void init (AttributeSet _attrs, int _style) {
    // font size
    m_score_font *= m_density;
    m_dlg_font *= m_density;

    // init painter
    m_painter = new Paint();
    m_painter.setStyle(Paint.Style.FILL);

    // text painter: anti-alias/bold
    m_txt_painter = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.FAKE_BOLD_TEXT_FLAG);
    m_txt_painter.setColor(g_txt_color);
    m_txt_painter.setTextSize(m_score_font);
    // m_txt_painter.setTypeface(Typeface.create("Arial", Typeface.ITALIC));
    m_txt_painter.setTypeface(Typeface.create("sans-serif-medium", Typeface.ITALIC));
  }

  public void start() {
    finalize();
    m_life = g_life;
    m_f16 = new F16();
    m_state = MyState.run;
    m_record = MyApp.get_record();
    postInvalidate();
  }
  
  public void pause() { 
    m_state = MyState.pause;
  }

  @Override
  protected void onDraw(Canvas _canvas) {
    /*
    // check click for each frame
    boolean b_single = false;
    if (m_ts_click > 0) {
      long now = System.currentTimeMillis();
      long lapse =  now - m_ts_click;
      // if not double
      if (lapse >= g_dclick_lapse) {
        b_single = true;
        // reset
        m_ts_click = m_ts_up = m_ts_down = -1;
      }
    }
    // process click evt
    if (b_single)
      on_click(m_click_pos_x, m_click_pos_y);
    */

    // draw
    super.onDraw(_canvas);
    switch (m_state) {
      case   run: dojob_draw_running(_canvas); break;
      case pause:   dojob_draw_pause(_canvas); break;
      case  dead:    dojob_draw_stop(_canvas); break;

      default:
        break;
    }
  }

  // draw roles in fixed pos
  private void dojob_draw_pause(Canvas _canvas) {
    // life
    dojob_draw_life(_canvas);
    // score
    dojob_draw_score(_canvas);

    // calling AnimateBase::onUpdate, instead of draw(),
    // so we can render static/fixed ani, without changing pos
    for (AnimateBase role:m_ani_list) {
      role.onUpdate(_canvas, m_painter, this);
    }

    if (m_f16 != null)
      m_f16.onUpdate(_canvas, m_painter, this);

    // draw score dlg
    dojob_draw_dlg(_canvas, "Resume");
    // if (m_ts_click > 0)
    //   postInvalidate();
  }

  private void dojob_draw_running(Canvas _canvas) {
    // life
    dojob_draw_life(_canvas);
    // score
    dojob_draw_score(_canvas);

    // 1st time draw: move self to bottom/center of screen
    if (m_frame_id == 0) {
      float x = _canvas.getWidth()/2;
      float y = _canvas.getHeight() - m_f16.h()/2;
      m_f16.moveCtrTo(x, y);
    }

    // add cached roles
    if (m_ani_list_cache.size() > 0) {
      m_ani_list.addAll(m_ani_list_cache);
      m_ani_list_cache.clear();
    }

    // check/clean the bullets lower than self/f16
    dojob_clear_bullets();

    // clear the finalizeed roles
    dojob_clear_roles();

    // add new role every 30 frame
    if (m_frame_id % 30 == 0) {
      AnimateBase role = Target.gen(this, m_frame_id, _canvas.getWidth(), m_level);
      if (role != null)
        add(role);
    }
    m_frame_id++;

    // roles
    Iterator<AnimateBase> it = m_ani_list.iterator();
    while (it.hasNext()) {
      AnimateBase role = it.next();
      if (!role.dead()) {
        // note: AnimateBase::draw could call finalize()
        role.draw(_canvas, m_painter, this);
      }

      // check if finalize in ::draw
      if (role.dead())
        it.remove();
    }

    // draw self
    if (m_f16 != null) {
      m_f16.draw(_canvas, m_painter, this);
      // f16 dead -> game over
      if (m_f16.dead()) {
        // check life
        m_life--;
        if (m_life > 0) {
          // new f16
          m_f16 = new F16();
          // initial pos
          float x = _canvas.getWidth()/2;
          float y = _canvas.getHeight() - m_f16.h()/2;
          m_f16.moveCtrTo(x, y);
        }
        else {
          m_state = MyState.dead;
          MyApp.new_score(m_score);
        }
      }
      // call this to make sure View would render continuously
      postInvalidate();
    }
  }

  // draw state: stopped
  private void dojob_draw_stop(Canvas _canvas) {
    dojob_draw_dlg(_canvas, "New Game");
    // if (m_ts_click > 0)
    //   postInvalidate();
  }

  // draw icon for each life on screen corner
  private void dojob_draw_life(Canvas _canvas) {
    float margin = 5*m_density;
    float pad = 5*m_density;
    //
    Bitmap bmp = MyApp.getBmp(BmpMngr.id_self);
    int bmp_w = (int)bmp.getWidth();
    int bmp_h = (int)bmp.getHeight();
    Rect src = new Rect(0, 0, bmp_w, bmp_h);
    //
    float x = margin*m_density;
    float y = margin*m_density;
    float w = 20*m_density;
    float h = 20*m_density;

    for (int i=0; i<m_life-1; i++) { // not inluding curret using f16
      x += i*(pad+w);
      RectF dst = new RectF(x, y, x+w, y+h);
      _canvas.drawBitmap(bmp, src, dst, m_painter);
    }
  }

  // draw dlg
  private void dojob_draw_dlg(Canvas _canvas, String _str) {
    // screen size
    int screen_w = _canvas.getWidth();
    int screen_h = _canvas.getHeight();

    // save ori font/align/style/color
    float old_font = m_txt_painter.getTextSize();
    Paint.Align old_align = m_txt_painter.getTextAlign();
    int old_color = m_painter.getColor();
    Paint.Style old_style = m_painter.getStyle();

    // pos
    int   margin_w = (int)(screen_w*0.055);
    int margin_top = (int)(screen_h*0.23);

    int dlg_w = screen_w-margin_w*2;
    int dlg_h =(int)(screen_h*0.245);

    // row 1
    int title_h = (int)(screen_h*0.11);
    // row 2
    int score_h = (int)(screen_h*0.11);

    // row 3
    int btn_w = (int)(screen_w*0.4);
    int btn_h = (int)(screen_h*0.08);
    int btn_margin_top = (int)(screen_h*0.15);

    // move cursor
    _canvas.translate(margin_w, margin_top);

    // draw: bg
    m_painter.setStyle(Paint.Style.FILL);
    m_painter.setColor(g_bk_color);
    Rect rect_bg = new Rect(0, 0, dlg_w, dlg_h);
    _canvas.drawRect(rect_bg, m_painter);

    // draw title: "Score"
    m_txt_painter.setTextSize(m_dlg_font);
    m_txt_painter.setTextAlign(Paint.Align.CENTER);
    m_txt_painter.setColor(g_txt_color);
    float x = dlg_w/2;
    float y = (title_h-m_dlg_font)/2 + m_dlg_font;
    String str_record = "Highest Score: " + MyApp.get_record(); // XXX
    _canvas.drawText(str_record, x, y, m_txt_painter);

    // move cursor
    _canvas.translate(0, title_h);
    // draw score itself
    String str_curr = "Your Score: "+ m_score;
    x = dlg_w/2;
    y = (score_h-m_dlg_font)/2 + m_dlg_font;
    _canvas.drawText(str_curr, x, y, m_txt_painter);

    // move cursor
    _canvas.translate(0, score_h);
    // draw btn border
    Rect btn_rect = new Rect();
    btn_rect.left = (dlg_w-btn_w)/2;
    btn_rect.right = dlg_w-btn_rect.left;
    btn_rect.top = btn_margin_top;
    btn_rect.bottom = btn_rect.top + btn_h;
    _canvas.drawRect(btn_rect, m_painter);

    // draw btn text
    _canvas.translate(0, btn_rect.top);
    x = dlg_w/2;
    y = (btn_h-m_dlg_font)/2 + m_dlg_font;
    _canvas.drawText(_str, x, y, m_txt_painter);

    // restore ori settings
    m_txt_painter.setTextSize(old_font);
    m_txt_painter.setTextAlign(old_align);
    // restore
    m_painter.setStyle(old_style);
    m_painter.setColor(old_color);

    // rect for click checking
    m_btn_rect = new Rect(btn_rect);
    m_btn_rect.left = margin_w + btn_rect.left;
    m_btn_rect.right = m_btn_rect.left + btn_w;
    m_btn_rect.top = margin_top + title_h + score_h + btn_rect.top;
    m_btn_rect.bottom = m_btn_rect.top + btn_h;
  }

  private void dojob_draw_score(Canvas _canvas) {
    // boundary
    String txt = "Level: "+m_level+ "  Score: "+m_score + "";
    Rect bound = new Rect();
    m_txt_painter.getTextBounds(txt, 0, txt.length(), bound);

    int screen_w = _canvas.getWidth();
    float x = (screen_w - bound.width())/2;
    float y = 20*m_density + m_score_font  - m_score_font / 2;
    _canvas.drawText(txt, x, y, m_txt_painter);
  }

  // finalize 
  public void finalize() {
    m_state = MyState.dead;
    // reset
    m_frame_id = 0;
    m_score = 0;
    m_record = 0;
    m_level = 1;

    // finalize roles
    for (AnimateBase role:m_ani_list) {
      role.finalize();
    }
    m_ani_list.clear();

    // finalize f16/self
    if(m_f16 != null) {
      m_f16.finalize();
      m_f16 = null;
    }
  }

  // insert new animate role
  public void add(AnimateBase _role) {
    m_ani_list_cache.add(_role);
  }

  // clear the bullets behind the fighter/self
  private void dojob_clear_bullets() {
    if (m_f16 == null)
      return;

    float pos_y = m_f16.y();
    List<Bullet> list = getBullet();
    for (Bullet blt:list) {
      if (pos_y <= blt.y())
        blt.finalize();
    }
  }

  // clear finalizeed roles
  private void dojob_clear_roles() {
    Iterator<AnimateBase> it = m_ani_list.iterator();
    while (it.hasNext()) {
      AnimateBase role = it.next();
      if (role.dead())
        it.remove();
    }
  }

  // returns target list
  public List<Target> getTarget() {
    List<Target> list = new ArrayList<Target>();
    for (AnimateBase role:m_ani_list) {
      if (!role.dead() && role instanceof Target) {
        Target tgt = (Target)role;
        list.add(tgt);
      }
    }
    return list;
  }

  // returns bullet list
  public List<Bullet> getBullet() {
    List<Bullet> list = new ArrayList<Bullet>();
    for (AnimateBase role: m_ani_list) {
      if (!role.dead() && role instanceof Bullet) {
        Bullet bullet = (Bullet)role;
        list.add(bullet);
      }
    }
    return list;
  }

  @Override
  public boolean onTouchEvent(MotionEvent _evt) {
    // analyze it
    ClickType type = ClickType.unknown;

    m_click_pos_x = _evt.getX();
    m_click_pos_y = _evt.getY();
    long now = System.currentTimeMillis();
    int act = _evt.getAction();
    if (act == MotionEvent.ACTION_MOVE) {      // move
      long lapse = now - m_ts_down;
      if (lapse > g_click_lapse)
        type = ClickType.move;
    }
    else if (act == MotionEvent.ACTION_DOWN) { // down
      m_ts_down = now;
    }
    else if (act == MotionEvent.ACTION_UP) { // up
      m_ts_up = now;
      long lapse = m_ts_up - m_ts_down;
      if (lapse <= g_click_lapse) {
        // got a click
        type = ClickType.click;
        System.out.println("Got click. x: "+m_click_pos_x+" y: "+m_click_pos_y);
        
        // click in 3 sec after a big hitted
        if (m_ts_big_hitted > 0 && (now-m_ts_big_hitted) < 3000) {
          /* 
          float screen_w = _canvas.getWidth();
          float screen_h = _canvas.getHeight();
          if (m_click_pos_x<screen_w && m_click_pos_x > ((screen_w/3)*2)) {
          }
          */
          // easter egg !!!
          m_life++;
        }
        // reset
        m_ts_up = m_ts_down = -1;
      }
    }

    // proc it
    switch (m_state) {
      case run: 
      { // move
        if (type == ClickType.move && m_f16 != null) {
          m_f16.moveCtrTo(m_click_pos_x, m_click_pos_y);
        }
      } break;

      case pause:
      { // pause->run
        if (ClickType.click == type && m_btn_rect.contains((int)m_click_pos_x, (int)m_click_pos_y)) {
          m_state = MyState.run;
        }
        // if (m_ts_click > 0)
        postInvalidate();
      } break;

      case dead:
      { // dead->run
        if (type==ClickType.click && m_btn_rect.contains((int)m_click_pos_x, (int)m_click_pos_y)) {
          finalize();
          // start
          m_f16 = new F16();
          m_state = MyState.run;
          m_life = g_life;
          m_record = MyApp.get_record();
        }
        // if (m_ts_click > 0)
        postInvalidate();
      } break;

      default:
        break;
    }
    return true;
  }

  /*
  // analyze evts
  private ClickType dojob_proc_click(MotionEvent _evt) {
    ClickType type = ClickType.unknown;

    m_click_pos_x = _evt.getX();
    m_click_pos_y = _evt.getY();
    long now = System.currentTimeMillis();
    int act = _evt.getAction();
    if (act == MotionEvent.ACTION_MOVE) {      // move
      long lapse = now - m_ts_down;
      if (lapse > g_click_lapse)
        type = ClickType.move;
    }
    else if (act == MotionEvent.ACTION_DOWN) { // down
      m_ts_down = now;
    }
    else if (act == MotionEvent.ACTION_UP) { // up
      #if 0
        m_ts_up = now;
        long lapse = m_ts_up - m_ts_down;
        if (lapse <= g_click_lapse) {   // got a click
          // check if 2nd click
          long lap2 = m_ts_up - m_ts_click;
          if (lap2 <= g_dclick_lapse) {
            // got a dclick
            type = ClickType.dclick;
            // reset
            m_ts_click = m_ts_up = m_ts_down = -1;
          } else {
            m_ts_click = m_ts_up;
          }
        }
      #else
        m_ts_up = now;
        long lapse = m_ts_up - m_ts_down;
        if (lapse <= g_click_lapse) {
          type = ClickType.click;
          // reset
          m_ts_up = m_ts_down = -1;
        }
      #endif
    }
    return type;
  }

  private void on_click(float _x, float _y) {
    switch (m_state) {
      case run:
        break;

      case pause:
        if (m_btn_rect.contains((int)_x, (int)_y)) {
          m_state = MyState.run;
          postInvalidate();
        }
        break;

      case dead:
        if (m_btn_rect.contains((int)_x, (int)_y)) {
          finalize();
          // start
          m_f16 = new F16();
          m_state = MyState.run;
          m_life = g_life;
          m_record = MyApp.get_record();
          postInvalidate();
        }
        break;

      default:
        break;
    }
  }
  */
} // ---------- end of F16View

