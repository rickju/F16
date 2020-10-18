package com.leiju.shoot;
import com.leiju.shoot.F16View;
import android.app.Activity;
import android.os.Bundle;

public class F16Activity extends Activity {
  private F16View m_view;

  @Override
  protected void onCreate(Bundle _state) {
    super.onCreate(_state);
    setContentView(R.layout.activity_game);

    // XXX AppCtx.init(getContext());
    m_view = (F16View)findViewById(R.id.f16View);
    // m_view.start();
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (m_view != null) {
      m_view.pause();
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (m_view != null) {
      m_view.finalize();
      m_view = null;
    }
  }
}
