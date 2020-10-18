package com.leiju.shoot;

import android.graphics.Paint;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.graphics.Rect;
import android.graphics.Point;

// base
class AnimateBase {
  protected Bitmap m_bmp = null;

  private boolean m_b_dead = false;
  private boolean m_b_visible = true;

  private float m_pos_x = 0;
  private float m_pos_y = 0;
  private   int m_frame_id = 0;

  // contructor
  public AnimateBase(Bitmap _bmp) {
    this.m_bmp = _bmp;
  }

  public void finalize() {
    m_bmp = null;
    m_b_dead = true;
  }

  // getters
  public boolean dead()          { return m_b_dead; }
  public  Bitmap getBmp()        { return m_bmp; }
  public boolean is_visible()    { return m_b_visible; }
  public   float x()             { return m_pos_x; }
  public   float y()             { return m_pos_y; }
  public     int frame_id()      { return m_frame_id; }

  public float w() {
    if (m_bmp == null)
      return 0;
    return m_bmp.getWidth();
  }

  public float h() {
    if (m_bmp == null)
      return 0;
    return m_bmp.getHeight();
  }

  public void set_x (float _x) { this.m_pos_x = _x; }
  public void set_y (float _y) { this.m_pos_y = _y; }

  public void show (boolean _visible) {
    this.m_b_visible = _visible;
  }

  public void move (float _x, float _y) {
    m_pos_x += _x;
    m_pos_y += _y;
  }

  public void moveTo (float _x, float _y) {
    this.m_pos_x = _x;
    this.m_pos_y = _y;
  }

  public void moveCtrTo (float _x, float _y) {
    m_pos_x = _x - w()/2;
    m_pos_y = _y - h()/2;
  }

  public RectF getRect() {
    float left = m_pos_x;
    float top = m_pos_y;
    float right = left + w();
    float bottom = top + h();
    RectF rectF = new RectF(left, top, right, bottom);
    return rectF;
  }

  public Rect getImgRect() {
    Rect rect = new Rect(0, 0, (int)w(), (int)h());
    return rect;
  }

  public Point hitPos(AnimateBase _dst) {
    Point p = null;
    RectF src = getRect();
    RectF dst = _dst.getRect();
    RectF rectF = new RectF();
    boolean b_hit = rectF.setIntersect(src, dst);
    if (b_hit) {
      p = new Point(Math.round(rectF.centerX()), Math.round(rectF.centerY()));
    }
    return p;
  }

  public final void draw(Canvas _c, Paint _paint, F16View _view) {
    m_frame_id++;
    onUpdatePrep(_c, _paint, _view);
    onUpdate(_c, _paint, _view);
    onUpdateDone(_c, _paint, _view);
  }

  protected void onUpdatePrep (Canvas _c, Paint _p, F16View _v) {}
  protected void onUpdateDone (Canvas _c, Paint _p, F16View _v) {}
  public void onUpdate(Canvas _c, Paint _paint, F16View _view) {
    if (m_b_dead)
      return;
    if (null == this.m_bmp)
     return;
    if (!is_visible())
      return;

    // draw
    Rect src = getImgRect();
    RectF dst = getRect();
    _c.drawBitmap(m_bmp, src, dst, _paint);
  }
}

