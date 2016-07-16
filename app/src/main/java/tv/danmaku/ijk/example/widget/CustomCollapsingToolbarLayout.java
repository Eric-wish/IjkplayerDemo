package tv.danmaku.ijk.example.widget;
import android.support.design.widget.CollapsingToolbarLayout;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class CustomCollapsingToolbarLayout extends CollapsingToolbarLayout 
{
    public CustomCollapsingToolbarLayout(Context context,AttributeSet attrs){
        super(context,attrs);
        setTitleEnabled(false);
        setScrollContainer(false);
        
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        // TODO: Implement this method
        return false;
    }
    
    
    
    
}
