package com.leiju.shoot;
import java.util.List;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;

enum TargetType {
  small,
  normal,
  big
};

// cls Mover: moving up/down
class Mover extends AnimateBase {
  private float m_speed = 2; // positive means moving downward, negative for upward.

  public Mover(Bitmap bitmap) {
    super(bitmap);
  }

  public float getSpeed (){ return m_speed; }
  public  void setSpeed (float _speed){ this.m_speed = _speed; }

  @Override
  protected void onUpdatePrep(Canvas _canvas, Paint _paint, F16View _view) {
    if (dead())
      return;
    move(0, m_speed * _view.screen_den()); // moving m_speed pixels in Y axis
  }

  protected void onUpdateDone(Canvas _canvas, Paint _paint, F16View _view) {
    if (dead())
      return;
    // finalize once out of screen
    RectF screen_rect = new RectF(0, 0, _canvas.getWidth(), _canvas.getHeight());
    RectF rect = getRect();
    if (!RectF.intersects(screen_rect, rect))
      finalize();
  }
}

// cls Bullet. moving upward
class Bullet extends Mover {
  public Bullet(Bitmap _bmp) {
    super(_bmp);
    setSpeed(-10); // minus because moving upward
  }
}

// cls for target craft: always moving downward
class Target extends Mover {
  protected static int g_default_speed = 2;
  protected static double g_high_speed_ratio = 1.0/3.0; // 1/3 chance high speed

  protected int m_credit = 1; // default score
  protected int  m_armor = 1; // default armor

  // constructor
  private Target(Bitmap _bmp, int _arm, int _credit) {
    super(_bmp);
    m_armor = _arm;
    m_credit = _credit;
  }

  @Override
  protected void onUpdateDone(Canvas _canvas, Paint _paint, F16View _view) {
    super.onUpdateDone(_canvas, _paint, _view);
    // check alive
    if (dead())
      return;
    // check hitted
    List<Bullet> list = _view.getBullet();
    for (Bullet blt :list) {
      Point p = hitPos(blt);
      if (p == null)
        continue;
      // hitted
      blt.finalize();
      m_armor--;
      if (m_armor <= 0) {
        explode(_view);
        break;
      }
    } // for
  }

  // explode and finalize the target
  public void explode(F16View _view) {
    // gen explode role
    float x = x() + w()/2;
    float y = y() + h()/2;
    Explode epl = new Explode();
    epl.moveCtrTo(x, y);
    _view.add(epl);
    // add score
    _view.score(m_credit);
    // kill after explode done
    finalize();
  }

  private static TargetType rand_type() {
    // small: 12/20, type normal: 7/20, type big: 1/20
    double lev_1 = 0.0, lev_2 = 7.0/20.0, lev_3 = (double)1.0/20.0;
    lev_1 = 1 - lev_2 - lev_3;
    lev_2 += lev_1;
    lev_3 += lev_2;
    // double rand = (int)Math.floor(Math.random());
    double rand = Math.random();
    // System.out.println("lev 1: "+lev_1+", lev 2: "+lev_2+ ", lev_3: "+lev_3);
    // System.out.println("rand: "+rand);
    if (rand <= lev_1)
      return TargetType.small;
    else if (rand <= lev_2)
      return TargetType.normal;
    else
      return TargetType.big;
  }

  // gen a role randomly
  public static AnimateBase gen (F16View _view, long _frame_id, int _screen_w, int _level) {
    AnimateBase role = null;
    // called num of dojob_gen 
    int called = Math.round(_frame_id/30);
    // rand target type
    TargetType type = rand_type();
    // gen new targets: every 25 frame
    if ((called+1)%25 != 0) {
      // rand size
      int bmp = 4, arm = 1, crd = 1;
      switch (type) {
        case  small: bmp = BmpMngr.id_small;  arm=1;  crd=1; break; // small
        case normal:   bmp = BmpMngr.id_mid;  arm=4;  crd=6; break; //
        case    big:  bmp =  BmpMngr.id_big; arm=10; crd=30; break; // strong
        default:
          break;
      }
      role = new Target(MyApp.getBmp(bmp), arm, crd);
    }
    // none
    if (role == null)
      return null;

    double rand = Math.random();
    float w = role.w();
    float h = role.h();

    // rand pos
    float x = (float)((_screen_w-w)*rand);
    float y = -h;
    role.set_x(x);
    role.set_y(y);

    // speed up 120% each level
    float level_ratio = 1;
    for (int i=0; i<_level; i++)
      level_ratio *= 1.2;
    float speed = g_default_speed*level_ratio;
    // double speed: 1/3
    boolean b_double = Math.random() < g_high_speed_ratio;
    if (type != TargetType.big && b_double)
      speed = speed*2;
    if (role instanceof Mover) {
      Mover m = (Mover)role;
      m.setSpeed(speed);
    }
    return role;
  }
} // endof Target

