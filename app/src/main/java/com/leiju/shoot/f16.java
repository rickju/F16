package com.leiju.shoot;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;

import java.util.List;

// the F16/self class
class F16 extends AnimateBase {
  private final int g_shoot_fps = 10;
  private final int g_flash_num = 7; // flash 7 times
  private final int g_flash_fps = 8; // hide/show every 8 frame

  private boolean m_b_hitted = false;
  private int m_flash_start = 0;
  private int m_flash_end = 0;

  public F16() {
    super(MyApp.getBmp(BmpMngr.id_self));
  }

  @Override
  protected void onUpdatePrep(Canvas _canvas, Paint _paint, F16View _view) {
    if (dead())
      return;
    // check pos is in screen
    if (x() < 0)
      set_x(0);
    if (y() < 0)
      set_y(0);

    RectF rect = getRect();
    int screen_w = _canvas.getWidth();
    int screen_h = _canvas.getHeight();
    if (rect.right > screen_w)
      set_x(screen_w - w());
    if (rect.bottom > screen_h)
      set_y(screen_h - h());

    // shoot
    shoot(_view);
  }

  // shoot
  public void shoot(F16View _view) {
    if (dead())
      return;
    if (m_b_hitted || dead())
      return;
    // shoot every 10 frames
    if (0 != frame_id() % g_shoot_fps)
      return;

    // initial pos of bullet
    float x = x() + w() / 2;
    float y = y() - 5;
    // add bullet
    Bitmap bmp = MyApp.getBmp(BmpMngr.id_bullet);
    Bullet blt = new Bullet(bmp);
    blt.moveTo(x, y);
    _view.add(blt);
  }

  // after hitted: 1. explode  2. flash 3. kill self
  protected void onUpdateDone(Canvas _canvas, Paint _paint, F16View _view) {
    if (dead())
      return;
    // check if hitted in advance
    if (!m_b_hitted) {
      List<Target> list = _view.getTarget();
      for (Target target: list) {
        Point p = hitPos(target);
        if (p != null) { // hitted
          // dead
          explode(_view);
          break;
        }
      }
    }

    if (m_flash_start <= 0)
      return;
    long curr = frame_id();
    if (curr < m_flash_start)
      return;

    // keep
    if (0 != ((curr-m_flash_start)%g_flash_fps))
      return;
    // flashing
    show(!is_visible());
    // kill once flash done
    if (curr > m_flash_end) {
      // kill self
      finalize();
    }
    return;
  }

  // self/f16 hitted and explode
  private void explode(F16View _view) {
    if (m_b_hitted)
      return;
    m_b_hitted = true;
    // hide
    show(false);
    // create explode
    float x = x() + w()/2;
    float y = y() + h()/2;
    Explode epl = new Explode();
    epl.moveCtrTo(x, y);
    _view.add(epl);
    m_flash_start = frame_id() + epl.frame_id();
    m_flash_end = m_flash_start + g_flash_num*g_flash_fps;
  }
} // end of f16
