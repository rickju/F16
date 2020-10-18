package com.leiju.shoot;
import com.leiju.shoot.R;
import com.leiju.shoot.F16;

import android.content.SharedPreferences;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.util.AttributeSet;
import android.view.View;
import android.content.res.TypedArray;
import android.text.TextPaint;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import android.app.Application;

// howto:
//  xml: <application android:name="com.xyz.MyApp"> </application>
// everywhere call MyApp.getAppContext() to get your application context statically.
//
public class MyApp extends Application {
  private static Context g_ctx;
  private static SharedPreferences g_pref;

  public void onCreate() {
    super.onCreate();
    MyApp.g_ctx= getApplicationContext();
    g_pref = g_ctx.getSharedPreferences("MyPref", 0);
  }

  public static Context getAppContext() {
    return MyApp.g_ctx;
  }

  // bmp
  static BmpMngr g_bmp_mngr = null;
  public static Bitmap getBmp(int _id) { 
    if (g_bmp_mngr == null)
      g_bmp_mngr = new BmpMngr();
    return g_bmp_mngr.getBmp(_id);
  }

  // score
  public static int get_record() {
    // String name_list = g_pref.getString("sssn_name_list", "");
    int record = g_pref.getInt("score_record", 0);
    return record;
  }
  public static void new_score(int _score) {
    int old_record = get_record();
    if (old_record >= _score)
      return;

    // save 
    SharedPreferences.Editor editor = g_pref.edit();
    // System.out.println("write sssn_name_list: "+name_list);
    // editor.putString("sssn_name_list", name_list);
    editor.putInt("score_record", _score);
    editor.commit();
  }
}

/*
// singleton: app context 
class AppCtx {
  protected Context m_ctx;
  protected static AppCtx g_ctx;
  public static Context get() {
    if (g_ctx == null)
      return null;
    return g_ctx.m_ctx;
  }

  public static void init (Context _ctx) {
    if (g_ctx == null)
      g_ctx = new AppCtx();
    if (g_ctx.m_ctx == null)
      g_ctx.m_ctx = _ctx;
  }

}
*/

class BmpMngr {
  public static final int    id_self = 0;
  public static final int id_explode = 1;
  public static final int  id_bullet = 2;
  public static final int   id_small = 3;
  public static final int     id_mid = 4;
  public static final int     id_big = 5;

  static int[] g_bmp_list = {
    R.drawable.a60,           // 0 f16/self
    R.drawable.explode,       // 1
    R.drawable.bullet,        // 2
    R.drawable.d30,           // 3 small
    R.drawable.e60,           // 4 mid
    R.drawable.g80            // 5 big
  };

  ArrayList<Bitmap> m_bmp_list;

  // constructor
  public BmpMngr() {
    Context ctx = MyApp.getAppContext();
    m_bmp_list = new ArrayList<Bitmap>();
    for (int id: g_bmp_list) {
      Bitmap bmp = BitmapFactory.decodeResource(ctx.getResources(), id);
      m_bmp_list.add(bmp);
    }
  }

  public void finalize() {
    for (Bitmap bmp: m_bmp_list) {
      bmp.recycle();
    }
    m_bmp_list.clear();
  }

  public Bitmap getBmp(int _id) { 
    if (m_bmp_list == null)
      return null;
    return m_bmp_list.get(_id);
  }
}


