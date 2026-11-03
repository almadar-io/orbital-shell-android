package {{APP_PACKAGE}};

import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.util.Log;
import android.view.inputmethod.EditorInfo;

import com.google.androidgamesdk.GameActivity;
import com.google.androidgamesdk.OrbitalTextInput;
import com.google.androidgamesdk.OrbitalVideoInput;
import com.google.androidgamesdk.gametextinput.InputConnection;

public final class MainActivity extends GameActivity {
    private GestureDetector textMenuGesture;

    static {
        System.loadLibrary("orbital_client_dioxus");
    }

    @Override
    protected void onCreateSurfaceView() {
        super.onCreateSurfaceView();
        OrbitalTextInput.install(this, mSurfaceView);
        OrbitalVideoInput.install(this, mSurfaceView);
        registerForContextMenu(mSurfaceView);
        mSurfaceView.setLongClickable(false);
        textMenuGesture = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent event) {
                if (OrbitalTextInput.nativeIsEditableAt(event.getX(), event.getY())) {
                    openContextMenu(mSurfaceView);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        OrbitalVideoInput.clear();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        OrbitalVideoInput.pause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        OrbitalVideoInput.resume();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (textMenuGesture != null) textMenuGesture.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo info) {
        super.onCreateContextMenu(menu, view, info);
        InputConnection input = mSurfaceView.onCreateInputConnection(new EditorInfo());
        CharSequence selected = input == null ? null : input.getSelectedText(0);
        boolean hasSelection = selected != null && selected.length() > 0;
        Log.i("OrbitalTextInput", "Menu selection length=" + (selected == null ? -1 : selected.length()));
        menu.add(0, android.R.id.copy, 0, "Copy").setEnabled(hasSelection);
        menu.add(0, android.R.id.cut, 1, "Cut").setEnabled(hasSelection);
        menu.add(0, android.R.id.paste, 2, "Paste");
        menu.add(0, android.R.id.selectAll, 3, "Select all");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        InputConnection input = mSurfaceView.onCreateInputConnection(new EditorInfo());
        if (input == null) return super.onContextItemSelected(item);
        int id = item.getItemId();
        if (id == android.R.id.copy || id == android.R.id.cut || id == android.R.id.paste || id == android.R.id.selectAll) {
            boolean handled = input.performContextMenuAction(id);
            if (handled && id == android.R.id.selectAll) {
                mSurfaceView.post(() -> openContextMenu(mSurfaceView));
            }
            return handled;
        }
        return super.onContextItemSelected(item);
    }
}
