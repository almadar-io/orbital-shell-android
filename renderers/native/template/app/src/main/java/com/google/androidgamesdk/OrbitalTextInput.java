package com.google.androidgamesdk;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;
import android.view.inputmethod.EditorInfo;

import com.google.androidgamesdk.gametextinput.InputConnection;
import com.google.androidgamesdk.gametextinput.Settings;

public final class OrbitalTextInput {
    private OrbitalTextInput() {}

    public static native boolean nativeIsEditableAt(float x, float y);

    public static void install(GameActivity activity, GameActivity.InputEnabledSurfaceView view) {
        EditorInfo editorInfo = view.mInputConnection.getEditorInfo();
        Settings settings = new Settings(editorInfo, editorInfo.inputType == 0);
        view.mInputConnection = new ClipboardInputConnection(activity, view, settings).setListener(activity);
    }

    private static final class ClipboardInputConnection extends InputConnection {
        private final Context context;

        ClipboardInputConnection(Context context, GameActivity.InputEnabledSurfaceView view, Settings settings) {
            super(context, view, settings);
            this.context = context;
        }

        @Override
        public boolean performContextMenuAction(int id) {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (id == android.R.id.selectAll) {
                boolean selected = setSelection(0, getEditable().length());
                CharSequence text = getSelectedText(0);
                Log.i("OrbitalTextInput", "Select all=" + selected + " length=" + (text == null ? -1 : text.length()));
                return selected;
            }
            if (id == android.R.id.copy || id == android.R.id.cut) {
                CharSequence selected = getSelectedText(0);
                if (selected == null || selected.length() == 0) return false;
                clipboard.setPrimaryClip(ClipData.newPlainText("", selected));
                return id != android.R.id.cut || commitText("", 1);
            }
            if (id == android.R.id.paste) {
                ClipData clip = clipboard.getPrimaryClip();
                if (clip == null || clip.getItemCount() == 0) return false;
                return commitText(clip.getItemAt(0).coerceToText(context), 1);
            }
            return super.performContextMenuAction(id);
        }
    }
}
