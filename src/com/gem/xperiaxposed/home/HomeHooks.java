package com.gem.xperiaxposed.home;

import static com.gem.xperiaxposed.XposedMain.*;
import static de.robv.android.xposed.XposedBridge.*;
import static de.robv.android.xposed.XposedHelpers.*;

import java.util.Map;
import java.util.WeakHashMap;

import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;

import com.gem.xperiaxposed.AutoHook;
import com.sonymobile.flix.components.Component;
import com.sonymobile.flix.components.Image;
import com.sonymobile.flix.components.Scene;
import com.sonymobile.flix.util.Animation;
import com.sonymobile.grid.GridRect;
import com.sonymobile.home.HomeFragment;
import com.sonymobile.home.MainView;
import com.sonymobile.home.apptray.AppTray;
import com.sonymobile.home.apptray.AppTrayDrawerView;
import com.sonymobile.home.apptray.AppTrayPageIndicatorView;
import com.sonymobile.home.apptray.AppTrayPageView;
import com.sonymobile.home.apptray.AppTrayPresenter;
import com.sonymobile.home.apptray.AppTrayView;
import com.sonymobile.home.bitmap.MirrorBitmapDrawable;
import com.sonymobile.home.cui.CuiWidgetLoadHelper;
import com.sonymobile.home.data.Item;
import com.sonymobile.home.desktop.Desktop;
import com.sonymobile.home.desktop.DesktopView;
import com.sonymobile.home.folder.OpenFolderAdapter;
import com.sonymobile.home.presenter.view.AdvWidgetItemView;
import com.sonymobile.home.presenter.view.IconLabelView;
import com.sonymobile.home.presenter.view.ItemViewCreatorBase;
import com.sonymobile.home.stage.StageView;
import com.sonymobile.home.textview.TextViewUtilities;
import com.sonymobile.home.ui.pageview.PageViewInteractionListener;
import com.sonymobile.home.ui.widget.HomeAdvWidget;
import com.sonymobile.home.ui.widget.HomeAdvWidgetManager;
import com.sonymobile.home.ui.widget.HomeAppWidgetManager;
import com.sonymobile.ui.support.SystemUiVisibilityWrapper;

import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

////////////////////////////////////////////////////////////

public class HomeHooks
{

////////////////////////////////////////////////////////////

  @SuppressWarnings("unused")
  public static void hookTransparency(XC_LoadPackage.LoadPackageParam param)
  {
    final int system_ui_transparent_background = prefs.getInt("key_systemui_translucent_background", SYSTEM_UI_TRANSPARENT_BACKGROUND);
    new AutoHook()
    {
      public Object before_getSystemUiBackgroundColor(com.sonymobile.home.util.SystemUiExtensions thiz, Context c)
      {
        return system_ui_transparent_background;
      }
    };
    
    if(!prefs.getBoolean("key_menu_dark_bars", false))
    {
      new AutoHook()
      {
        public Object before_setSystemUiTransparent(AppTrayPresenter thiz, boolean b)
        {
          return VOID;
        }
      };
    }

    final boolean transparentDesktop = prefs.getBoolean("key_transparent_desktop", false);
    final boolean transparentDrawer = prefs.getBoolean("key_transparent_drawer", false);

    if(transparentDesktop || transparentDrawer)
    {
      new AutoHook()
      {
        public void after_all_constructors(MainView thiz)
        {
          setFullTransparent(thiz, transparentDesktop);
        }

        public void after_showApptray(MainView thiz, boolean show)
        {
          if(show)
            setFullTransparent(thiz, transparentDrawer);
        }
        
        public void after_showDesktop(MainView thiz, boolean show)
        {
          if(show)
            setFullTransparent(thiz, transparentDesktop);
        }

        public void after_setFocused(HomeFragment thiz, boolean focused)
        {
          if(focused)
          {
            MainView mainView = thiz.getMainView();
            if(mainView != null)
            {
              if((Boolean)callMethod(mainView, "isDesktopOpen"))
                setFullTransparent(mainView, transparentDesktop);
              else if((Boolean)callMethod(mainView, "isAppTrayOpen"))
                setFullTransparent(mainView, transparentDrawer);
            }
          }
        }
        
        public void before_apply(SystemUiVisibilityWrapper thiz)
        {
          Object mainView = getObjectField(thiz, "mView");
          if(mainView != null)
          {
            if((Boolean)callMethod(mainView, "isDesktopOpen"))
            {
              if(transparentDesktop)
                callMethod(thiz, "setFlag", SYSTEM_UI_FLAG_FULL_TRANSPARENCY, true);
            }
            else if((Boolean)callMethod(mainView, "isAppTrayOpen"))
            {
              if(transparentDrawer)
                callMethod(thiz, "setFlag", SYSTEM_UI_FLAG_FULL_TRANSPARENCY, true);
            }
          }
        }
        
        private void setFullTransparent(View view, boolean value)
        {
          if(value)
            view.setSystemUiVisibility(view.getSystemUiVisibility() | SYSTEM_UI_FLAG_FULL_TRANSPARENCY);
          else
            view.setSystemUiVisibility(view.getSystemUiVisibility() & ~SYSTEM_UI_FLAG_FULL_TRANSPARENCY);
        }
      };
    }

    if(transparentDrawer)
    {
      new AutoHook()
      {
        public void after_all_constructors(AppTrayView thiz)
        {
          setIntField(thiz, "mBackgroundColor", 0);
        }
      };
    }
  }
  
////////////////////////////////////////////////////////////

  @SuppressWarnings("unused")
  public static void hookFont(XC_LoadPackage.LoadPackageParam param)
  {
    if(prefs.getBoolean("key_condensed_font", false))
    {
      new AutoHook()
      {
        Typeface CONDENSED_FONT = Typeface.createFromFile("/system/fonts/RobotoCondensed-Regular.ttf");
        
        public void after_createTextView(TextViewUtilities thiz, Context c, float f, int i, MethodHookParam param)
        {
          ((TextView)param.getResult()).getPaint().setTypeface(CONDENSED_FONT);
        }
        
        public void after_createTextView(TextViewUtilities thiz, Context c, String s, float f, int i1, int i2, Rect r, Typeface t, int i3, MethodHookParam param)
        {
          ((TextView)param.getResult()).getPaint().setTypeface(CONDENSED_FONT);
        }
      };
    }
  }
  
////////////////////////////////////////////////////////////

  @SuppressWarnings("unused")
  public static void hookLayout(XC_LoadPackage.LoadPackageParam param)
  {
    final boolean desktop_disable_pagination = prefs.getBoolean("key_desktop_disable_pagination", false);
    final boolean drawer_disable_pagination = prefs.getBoolean("key_drawer_disable_pagination", false);
    if(desktop_disable_pagination || drawer_disable_pagination)
    {
      new AutoHook()
      {
        void after_onSceneCreated(MainView thiz, Scene scene, int i1, int i2)
        {
          if(desktop_disable_pagination)
          {
            Desktop desktop = (Desktop)getObjectField(thiz, "mDesktop");
            desktop.getView().removeChild(desktop.getPresenter().getPageIndicatorView());
          }
          if(drawer_disable_pagination)
          {
            AppTray appTray = (AppTray)getObjectField(thiz, "mAppTray");
            appTray.getView().removeChild(appTray.getPresenter().getPageIndicatorView());
          }
        }
      };
    }

    final boolean desktop_disable_labels = prefs.getBoolean("key_desktop_disable_labels", false);
    final boolean folder_disable_labels = prefs.getBoolean("key_folder_disable_labels", false);
    final boolean drawer_disable_labels = prefs.getBoolean("key_drawer_disable_labels", false);
    if(desktop_disable_labels || folder_disable_labels || drawer_disable_labels)
    {
      new AutoHook()
      {
        public Object after_includedLabel(ItemViewCreatorBase thiz, Item item)
        {
          String name = item.getPageViewName();
          if(desktop_disable_labels && "desktop".equals(name))
            return false;
          if(folder_disable_labels && "folder".equals(name))
            return false;
          if(drawer_disable_labels && "apptray".equals(name))
            return false;
          else
            return NONE;
        }

        public Object before_setMaxTextSize(IconLabelView thiz, int size)
        {
          if(! getBooleanField(thiz, "mIncludedLabel"))
            return VOID;
          else
            return NONE;
        }
      };
    }
  }
  
////////////////////////////////////////////////////////////
  
  @SuppressWarnings("unused")
  public static void hookDesktop(XC_LoadPackage.LoadPackageParam param)
  {
    final int desktop_animation = Integer.valueOf(prefs.getString("key_desktop_animation", "0"));
    if(desktop_animation != 0)
    {
      new AutoHook()
      {
        public void after_all_constructors(DesktopView thiz)
        {
          setIntField(thiz, "mAnimNbr", desktop_animation);
        }
      };
    }
  }
  
////////////////////////////////////////////////////////////

  @SuppressWarnings("unused")
  public static void hookDock(XC_LoadPackage.LoadPackageParam param)
  {
    if(prefs.getBoolean("key_disable_dock_stage", false))
    {
      new AutoHook()
      {
        public void after_updateBackground(StageView thiz)
        {
          ((Image)getObjectField(thiz, "mBackground")).setBitmap(null);
        }
      };
    }
      
    if(prefs.getBoolean("key_disable_dock_reflection", false))
    {
      new AutoHook()
      {
        public void after_all_constructors(MirrorBitmapDrawable thiz)
        {
          thiz.setMirrorEnabled(false);
        }
      };
    }
  }
  
////////////////////////////////////////////////////////////

  @SuppressWarnings("unused")
  public static void hookDrawer(XC_LoadPackage.LoadPackageParam param)
  {
    if(prefs.getBoolean("key_disable_drawer_backplate", false))
    {
      new AutoHook()
      {
        public void after_all_constructors(AppTrayPageView thiz)
        {
          thiz.getContent().removeChild((Component)getObjectField(thiz, "mDefaultBackplate"));
          thiz.getContent().removeChild((Component)getObjectField(thiz, "mUninstallBackplate"));
        }
      };
    }

    if(prefs.getBoolean("key_remember_drawer_page", false))
    {
      new AutoHook()
      {
        public Object before_gotoDefaultPage(AppTrayView thiz)
        {
          return VOID;
        }
      };
    }

    final int drawer_menu_opacity = prefs.getInt("key_drawer_menu_opacity", 100);
    if(drawer_menu_opacity != 100)
    {
      new AutoHook()
      {
        public void after_initialize(AppTrayDrawerView thiz, float f1, float f2, float f3)
        {
          ((View)getObjectField(thiz, "mListView")).getBackground().setAlpha((int)(2.55 * drawer_menu_opacity));
        }
      };
    }

    if(prefs.getBoolean("key_drawer_autohide_pagination", false))
    {
      new AutoHook()
      {
        public void after_all_constructors(AppTrayPageIndicatorView thiz)
        {
          thiz.setAutoHide(true);
        }
        
        public void after_setView(final AppTrayPresenter thiz, AppTrayView view)
        {
          view.addInteractionListener(new PageViewInteractionListener()
          {
            @Override
            public void onInteractionStart()
            {
              thiz.getPageIndicatorView().onInteractionStart();
            }
            @Override
            public void onInteractionEnd()
            {
              thiz.getPageIndicatorView().onInteractionEnd();
            }
          });
        }
        
        public void after_setTitle(final AppTrayPageIndicatorView thiz, String title)
        {
          thiz.onInteractionStart();
          ((Animation)getObjectField(thiz, "mPageIndicatorAnimation")).addListener(new Animation.Listener()
          {
            @Override
            public void onStart(Animation arg0)
            {
            }
            @Override
            public void onFinish(Animation arg0)
            {
              thiz.onInteractionEnd();
            }
          });
        }
        
        public Object before_onAppTrayDrawerVisibilityChanged(AppTrayPresenter thiz, float visibility)
        {
          callMethod(thiz, "setSystemUiTransparent", visibility <= 0.05);
          return VOID;
        }
      };
    }
  }

////////////////////////////////////////////////////////////

  @SuppressWarnings("unused")
  public static void hookFolders(XC_LoadPackage.LoadPackageParam param)
  {
    final int folderColumns = Integer.parseInt(prefs.getString("key_folder_columns", "4"));
    if(folderColumns != 4)
    {
      new AutoHook()
      {
        public Object before_setCellWidth(com.sonymobile.home.folder.GridView thiz, float width)
        {
          setFloatField(thiz, "mCellWidth", width * 4 / folderColumns);
          return VOID;
        }
      };
    }

    if(prefs.getBoolean("key_folder_multiline_labels", false))
    {
      new AutoHook()
      {
        public void after_getItemView(OpenFolderAdapter thiz, int i, MethodHookParam param)
        {
          try
          {
            setBooleanField(param.getResult(), "mCenterVertically", false);
          }
          catch(Throwable ex)
          {
          }
        }
        
        public Object after_getItemViewTextLines(ItemViewCreatorBase thiz, String s)
        {
          if("folder".equals(s))
            return 2;
          else
            return NONE;
        }
      };
    }
    
    if(prefs.getBoolean("key_folder_disable_background_dim", false))
    {
      try {
      findAndHookMethod("com.sonymobile.home.folder.OpenFolderView$DimAnimation", param.classLoader, "onUpdate", float.class, float.class, new XC_MethodReplacement() 
      {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable
        {
          return null;
        }
      });
      } catch(Throwable ex) { log(ex); }
    }
  }
  
////////////////////////////////////////////////////////////
  
  @SuppressWarnings("unused")
  public static void hookWidgets(XC_LoadPackage.LoadPackageParam param)
  {
    if(prefs.getBoolean("key_all_widgets_resizable", false))
    {
      new AutoHook()
      {
        private Map<HomeAdvWidget, int[]> advWidgetSizes = new WeakHashMap<HomeAdvWidget, int[]>();
        
        public Object after_getResizeMode(HomeAppWidgetManager thiz, int i)
        {
          return AppWidgetProviderInfo.RESIZE_BOTH;
        }

        public void after_createAppWidgetInfo(HomeAdvWidget thiz, PackageManager pm)
        {
          AppWidgetProviderInfo info = (AppWidgetProviderInfo)getObjectField(thiz, "mAppWidgetProviderInfo");
          info.resizeMode = AppWidgetProviderInfo.RESIZE_BOTH;
        }

        public void before_getVanillaSpanXY(CuiWidgetLoadHelper thiz, Context context, AppWidgetProviderInfo info)
        {
          info.resizeMode = AppWidgetProviderInfo.RESIZE_BOTH;
        }

        public void before_setAdvancedWidget(AdvWidgetItemView thiz, HomeAdvWidget w, HomeAdvWidgetManager wm, boolean b)
        {
          GridRect grid = thiz.getItem().getLocation().grid;
          advWidgetSizes.put(w, new int[] { grid.colSpan, grid.rowSpan });
        }
        
        public Object after_getSpanXY(HomeAdvWidget thiz)
        {
          int[] span = advWidgetSizes.get(thiz);
          if(span != null)
            return span;
          else
            return NONE;
        }
      };
    }
  } 
  
////////////////////////////////////////////////////////////
  
  public static void hookExperimental(XC_LoadPackage.LoadPackageParam param) throws Exception
  {
    if(prefs.getBoolean("key_enable_experimental", false))
      new com.gem.xperiaxposed.home.ExperimentalHooks();
  } 
  
////////////////////////////////////////////////////////////

}

////////////////////////////////////////////////////////////
