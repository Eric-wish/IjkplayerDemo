package tv.danmaku.ijk.example.widget;
import android.support.design.widget.AppBarLayout;
import android.util.AttributeSet;
import android.content.Context;
import android.view.MotionEvent;
import android.support.design.widget.CoordinatorLayout;
import android.view.View;

public class CustomAppBarLayout extends AppBarLayout
{
    public CustomAppBarLayout(Context context,AttributeSet attrs){
        super(context,attrs);
        
    }

    @Override//响应
    public boolean onTouchEvent(MotionEvent event)
    {
        // TODO: Implement this method
        return false;
        
    }

    @Override//拦截
    public boolean onInterceptTouchEvent(MotionEvent ev)
    {
        // TODO: Implement this method
        return false;
    }

    @Override//分发
    public boolean dispatchTouchEvent(MotionEvent ev)
    {
        // TODO: Implement this method
        return super.dispatchTouchEvent(ev);
       
    }
    
    
    public static class CustomScrollingViewBehavior extends ScrollingViewBehavior{
        
        @Override
        public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
            // We depend on any AppBarLayouts
            return dependency instanceof AppBarLayout;
        }
        
        
        @Override
        public boolean onDependentViewChanged(CoordinatorLayout parent, View child,
                                              View dependency) {
           /* offsetChildAsNeeded(parent, child, dependency);
            return false;*/
            return super.onDependentViewChanged(parent,child,dependency);
        }
        
        public float getOverlapRatioForOffset(View header){
            
            return super.getOverlapRatioForOffset(header);
        }
        
        
       public android.view.View findFirstDependency(java.util.List<android.view.View> views) {
           return super.findFirstDependency(views);
       }

       public int getScrollRange(android.view.View v) {
           
           return super.getScrollRange(v);
       }

        public CustomScrollingViewBehavior() {}

        public CustomScrollingViewBehavior(android.content.Context context, android.util.AttributeSet attrs) {
            super(context,attrs);
        }
        
        
        
  

        /*private void offsetChildAsNeeded(CoordinatorLayout parent, View child, View dependency) {
            final CoordinatorLayout.Behavior behavior =
                ((CoordinatorLayout.LayoutParams) dependency.getLayoutParams()).getBehavior();
            if (behavior instanceof Behavior) {
                // Offset the child, pinning it to the bottom the header-dependency, maintaining
                // any vertical gap, and overlap
                final Behavior ablBehavior = (Behavior) behavior;
                final int offset = ablBehavior.getTopBottomOffsetForScrollingSibling();
                child.offsetTopAndBottom((dependency.getBottom() - child.getTop())
                                         + ablBehavior.mOffsetDelta
                                         + getVerticalLayoutGap()
                                         - getOverlapPixelsForOffset(dependency));
            }
        }*/
        
    }
   
    
}
