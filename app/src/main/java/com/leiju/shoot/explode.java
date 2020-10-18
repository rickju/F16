package com.leiju.shoot;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

// cls for explode
class Explode extends AnimateBase {
  private final int m_fps =  2;    // draw 2 frame for each icon
  private final int m_max = 14;   // 14 icons in totoal
  private       int m_curr = 0;   // curr idx in 14 iconds

  public Explode() {
    super(MyApp.getBmp(BmpMngr.id_explode));
  }

  @Override
  public float w() {  // returning one-single-icon's width, instead of the whole w of 14 icons
    if (m_bmp == null)
      return 0;
    return m_bmp.getWidth()/m_max;
  }

  @Override
  public Rect getImgRect() {
    // bmp's (0, 0, w, h)
    Rect rect = super.getImgRect();
    // moving right to the curr icon
    int left = (int)(w()*m_curr);
    rect.offsetTo(left, 0);
    return rect;
  }

  @Override
  protected void onUpdateDone(Canvas _c, Paint _p, F16View _view) {
    if (dead())
      return;
    if (0 != frame_id() % m_fps)
      return;
    // next icon
    m_curr++;
    // finalize if done
    if (m_curr >= m_max)
      finalize();
  }
}

