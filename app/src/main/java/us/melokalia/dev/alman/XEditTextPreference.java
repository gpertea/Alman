package us.melokalia.dev.alman;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
/**
 * Created by gpertea on 2/25/2017.
 */

public final class XEditTextPreference extends EditTextPreference {
    public XEditTextPreference(final Context ctx, final AttributeSet attrs)  {
        super(ctx, attrs);
    }
    public XEditTextPreference(final Context ctx)  {
        super(ctx);
    }
    @Override
    public void setText(final String value)  {
        super.setText(value);
        setSummary(getText());
    }
}
