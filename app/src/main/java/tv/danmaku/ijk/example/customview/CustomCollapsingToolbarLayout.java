package tv.danmaku.ijk.example.customview;
import android.support.design.widget.CollapsingToolbarLayout;
import android.content.Context;
import android.util.AttributeSet;

public class CustomCollapsingToolbarLayout extends CollapsingToolbarLayout 
{
    public CustomCollapsingToolbarLayout(Context context,AttributeSet attrs){
        super(context,attrs);
        setTitleEnabled(false);
        setScrollContainer(false);
        
    }
    
    
}
