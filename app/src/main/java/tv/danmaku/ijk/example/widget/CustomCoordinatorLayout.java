package tv.danmaku.ijk.example.widget;
import android.support.design.widget.CoordinatorLayout;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class CustomCoordinatorLayout extends CoordinatorLayout
{
    public CustomCoordinatorLayout(Context context,AttributeSet attrs){
        super(context,attrs);
       
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev)
    {
        // TODO: Implement this method
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev)
    {
        // TODO: Implement this method
        return false;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev)
    {
        // TODO: Implement this method
        return super.dispatchTouchEvent(ev);
    }

   
    
    
}
