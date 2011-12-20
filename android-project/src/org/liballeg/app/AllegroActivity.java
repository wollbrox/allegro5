package org.liballeg.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.hardware.*;
import android.content.res.Configuration;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ActivityInfo;
import android.util.Log;

import java.lang.String;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.Runnable;

import java.util.List;
import java.util.BitSet;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.egl.*;

public class AllegroActivity extends Activity implements SensorEventListener
{
   /* properties */
   
   static final int ALLEGRO_DISPLAY_ORIENTATION_UNKNOWN = 0;
   static final int ALLEGRO_DISPLAY_ORIENTATION_0_DEGREES = 1;
   static final int ALLEGRO_DISPLAY_ORIENTATION_90_DEGREES = 2;
   static final int ALLEGRO_DISPLAY_ORIENTATION_180_DEGREES = 4;
   static final int ALLEGRO_DISPLAY_ORIENTATION_270_DEGREES = 8;
   static final int ALLEGRO_DISPLAY_ORIENTATION_PORTRAIT = 5;
   static final int ALLEGRO_DISPLAY_ORIENTATION_LANDSCAPE = 10;
   static final int ALLEGRO_DISPLAY_ORIENTATION_ALL = 15;
   static final int ALLEGRO_DISPLAY_ORIENTATION_FACE_UP = 16;
   static final int ALLEGRO_DISPLAY_ORIENTATION_FACE_DOWN = 32;
   
   private static SensorManager sensorManager;
   private List<Sensor> sensors;
   
   private static AllegroSurface surface;
   private Handler handler;
   
   private Configuration currentConfig;
   
   /* native methods we call */
   public native boolean nativeOnCreate();
   public native void nativeOnPause();
   public native void nativeOnResume();
   public native void nativeOnDestroy();
   public native void nativeOnAccel(int id, float x, float y, float z);

   public native void nativeCreateDisplay();
   
   public native void nativeOnOrientationChange(int orientation, boolean init);
   
   /* load allegro */
   static {
		/* FIXME: see if we can't load the allegro library name, or type from the manifest here */
      System.loadLibrary("allegro-debug");
      System.loadLibrary("allegro_primitives-debug");
   }
   
   public static AllegroActivity Self;

   /* methods native code calls */
   
   public String getLibraryDir()
   {
      /* Android 1.6 doesn't have .nativeLibraryDir :( */
		/* FIXME: use reflection here to detect the capabilities of the device */
      return getApplicationInfo().dataDir + "/lib";
   }
   
   public String getAppName()
   {
      try {
         return getPackageManager().getActivityInfo(getComponentName(), android.content.pm.PackageManager.GET_META_DATA).metaData.getString("org.liballeg.app_name");
      } catch(PackageManager.NameNotFoundException ex) {
         return new String();
      }
   }
   
   public String getDataDir()
   {
      return getApplicationInfo().dataDir;
   }
   
   public void postRunnable(Runnable runme)
   {
      try {
         Log.d("AllegroActivity", "postRunnable");
         handler.post( runme );
      } catch (Exception x) {
         Log.d("AllegroActivity", "postRunnable exception: " + x.getMessage());
      }
   }
   
   public void createSurface()
   {
      try {
         Log.d("AllegroActivity", "createSurface");
         if(surface == null) {
            surface = new AllegroSurface(getApplicationContext());
         }
         
         SurfaceHolder holder = surface.getHolder();
         holder.addCallback(surface); 
         holder.setType(SurfaceHolder.SURFACE_TYPE_GPU);
         //setContentView(surface);
         Window win = getWindow();
         win.setContentView(surface);
         Log.d("AllegroActivity", "createSurface end");  
      } catch(Exception x) {
         Log.d("AllegroActivity", "createSurface exception: " + x.getMessage());
      }
   }
   
   public void postCreateSurface()
   {
      try {
         Log.d("AllegroActivity", "postCreateSurface");
         
         handler.post( new Runnable() {
            public void run() {
               createSurface();
            }
         } );
      } catch(Exception x) {
         Log.d("AllegroActivity", "postCreateSurface exception: " + x.getMessage());
      }
      
      return;
   }
   
   public void destroySurface()
   {
      Log.d("AllegroActivity", "destroySurface");
      
      ViewGroup vg = (ViewGroup)(surface.getParent());
      vg.removeView(surface);
      surface = null;
   }
   
   public void postDestroySurface(View s)
   {
      try {
         Log.d("AllegroActivity", "postDestroySurface");
         
         handler.post( new Runnable() {
            public void run() {
               destroySurface();
            }
         } );
      } catch(Exception x) {
         Log.d("AllegroActivity", "postDestroySurface exception: " + x.getMessage());
      }
      
      return;
   }
   
   public int getNumSensors() { return sensors.size(); }
   
   public void postFinish()
   {
      try {
         Log.d("AllegroActivity", "posting finish!");
         handler.post( new Runnable() {
            public void run() {
               try {
                  AllegroActivity.this.finish();
               } catch(Exception x) {
                  Log.d("AllegroActivity", "inner exception: " + x.getMessage());
               }
            }
         } );
      } catch(Exception x) {
         Log.d("AllegroActivity", "exception: " + x.getMessage());
      }
   }
   
   /* end of functions native code calls */
   
   
   /** Called when the activity is first created. */
   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      
      Self = this;
      
      Log.d("AllegroActivity", "onCreate");
      
      handler = new Handler();
      initSensors();
      
      currentConfig = getResources().getConfiguration();
      
      Log.d("AllegroActivity", "before nativeOnCreate");
      if(!nativeOnCreate()) {
         finish();
         Log.d("AllegroActivity", "onCreate fail");
         return;
      }
      
      nativeOnOrientationChange(getAllegroOrientation(currentConfig), true);
      
      Log.d("AllegroActivity", "onCreate end");
   }
   
   @Override
   public void onStart()
   {
      super.onStart();
      Log.d("AllegroActivity", "onStart.");
   } //
   
   @Override
   public void onRestart()
   {
      super.onRestart();
      Log.d("AllegroActivity", "onRestart.");
   }
   
   @Override
   public void onStop()
   {
      super.onStop();
      Log.d("AllegroActivity", "onStop.");
   }
   
   /** Called when the activity is paused. */
   @Override
   public void onPause()
   {
      super.onPause();
      Log.d("AllegroActivity", "onPause");
      
      disableSensors();
      
      nativeOnPause();
      Log.d("AllegroActivity", "onPause end");
   }
   
   /** Called when the activity is resumed/unpaused */
   @Override
   public void onResume()
   {
      Log.d("AllegroActivity", "onResume");
      super.onResume();
      
      enableSensors();
   
      nativeOnResume();
      
      Log.d("AllegroActivity", "onResume end");
   }
   
   /** Called when the activity is destroyed */
   @Override
   public void onDestroy()
   {
      super.onDestroy();
      Log.d("AllegroActivity", "onDestroy");
      
      nativeOnDestroy();
      Log.d("AllegroActivity", "onDestroy end");
   }
   
   /** Called when config has changed */
   @Override
   public void onConfigurationChanged(Configuration conf)
   {
      super.onConfigurationChanged(conf);
      Log.d("AllegroActivity", "onConfigurationChanged");
      // compare conf.orientation with some saved value
      
      int changes = currentConfig.diff(conf);
      
      if((changes & ActivityInfo.CONFIG_FONT_SCALE) != 0)
         Log.d("AllegroActivity", "font scale changed");

      if((changes & ActivityInfo.CONFIG_MCC) != 0)
         Log.d("AllegroActivity", "mcc changed");
      
      if((changes & ActivityInfo.CONFIG_MNC) != 0)
         Log.d("AllegroActivity", " changed");
      
      if((changes & ActivityInfo.CONFIG_LOCALE) != 0)
         Log.d("AllegroActivity", "locale changed");
      
      if((changes & ActivityInfo.CONFIG_TOUCHSCREEN) != 0)
         Log.d("AllegroActivity", "touchscreen changed");
      
      if((changes & ActivityInfo.CONFIG_KEYBOARD) != 0)
         Log.d("AllegroActivity", "keyboard changed");
      
      if((changes & ActivityInfo.CONFIG_NAVIGATION) != 0)
         Log.d("AllegroActivity", "navigation changed");
         
      if((changes & ActivityInfo.CONFIG_ORIENTATION) != 0) {
         Log.d("AllegroActivity", "orientation changed");
         nativeOnOrientationChange(getAllegroOrientation(conf), false);
      }
         
      if((changes & ActivityInfo.CONFIG_SCREEN_LAYOUT) != 0)
         Log.d("AllegroActivity", "screen layout changed");
      
      if((changes & ActivityInfo.CONFIG_SCREEN_SIZE) != 0)
         Log.d("AllegroActivity", "screen size changed");
      
      if((changes & ActivityInfo.CONFIG_SMALLEST_SCREEN_SIZE) != 0)
         Log.d("AllegroActivity", "smallest screen size changed");
      
      if((changes & ActivityInfo.CONFIG_UI_MODE) != 0)
         Log.d("AllegroActivity", "ui mode changed");
      
      if(currentConfig.screenLayout != conf.screenLayout) {
         Log.d("AllegroActivity", "screenLayout changed!");
      }
   }
   
   /** Called when app is frozen **/
   @Override
   public void onSaveInstanceState(Bundle state)
   {
      Log.d("AllegroActivity", "onSaveInstanceState");
      /* do nothing? */
      /* this should get rid of the following warning:
       *  couldn't save which view has focus because the focused view has no id. */
   }
   
   /* sensors */
   
   private boolean initSensors()
   {
      sensorManager = (SensorManager)getApplicationContext().getSystemService("sensor");
      
      /* only check for Acclerometers for now, not sure how we should utilize other types */
      sensors = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
      if(sensors == null)
         return false;
      
      return true;
   }
   
   private void enableSensors()
   {
      for(int i = 0; i < sensors.size(); i++) {
         sensorManager.registerListener(this, sensors.get(i), SensorManager.SENSOR_DELAY_GAME);
      }
   }
   
   private void disableSensors()
   {
      for(int i = 0; i < sensors.size(); i++) {
         sensorManager.unregisterListener(this, sensors.get(i));
      }
   }
   
   public void onAccuracyChanged(Sensor sensor, int accuracy) { /* what to do? */ }
   
   public void onSensorChanged(SensorEvent event)
   {
      int idx = sensors.indexOf(event.sensor);
      
      if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
         nativeOnAccel(idx, event.values[0], event.values[1], event.values[2]);
      }
   }
   
   private int getAllegroOrientation(Configuration conf)
   {
      int allegro_orientation = ALLEGRO_DISPLAY_ORIENTATION_UNKNOWN;
      switch(conf.orientation) {
         case Configuration.ORIENTATION_LANDSCAPE:
            allegro_orientation = ALLEGRO_DISPLAY_ORIENTATION_LANDSCAPE;
            break;
            
         case Configuration.ORIENTATION_PORTRAIT:
            allegro_orientation = ALLEGRO_DISPLAY_ORIENTATION_PORTRAIT;
            break;
            
         case Configuration.ORIENTATION_SQUARE:
            // XXX: maybe add a ALLEGRO_DISPLAY_ORIENTATION_SQUARE?
            // allegro_orientation = ALLEGRO_DISPLAY_ORIENTATION_SQUARE;
            break;
            
         case Configuration.ORIENTATION_UNDEFINED:
            break;
      }
      return allegro_orientation;
   }
}

class AllegroSurface extends SurfaceView implements SurfaceHolder.Callback, 
   View.OnKeyListener, View.OnTouchListener
{
   
   static final int ALLEGRO_PIXEL_FORMAT_RGBA_8888 = 10;
   static final int ALLEGRO_PIXEL_FORMAT_RGB_888 = 12;
   static final int ALLEGRO_PIXEL_FORMAT_RGB_565 = 13;
   static final int ALLEGRO_PIXEL_FORMAT_RGBA_5551 = 15;
   static final int ALLEGRO_PIXEL_FORMAT_RGBA_4444 = 26;

   static final int ALLEGRO_KEY_A     = 1;
   static final int ALLEGRO_KEY_B     = 2;
   static final int ALLEGRO_KEY_C     = 3;
   static final int ALLEGRO_KEY_D     = 4;
   static final int ALLEGRO_KEY_E     = 5;
   static final int ALLEGRO_KEY_F     = 6;
   static final int ALLEGRO_KEY_G     = 7;
   static final int ALLEGRO_KEY_H     = 8;
   static final int ALLEGRO_KEY_I     = 9;
   static final int ALLEGRO_KEY_J     = 10;
   static final int ALLEGRO_KEY_K     = 11;
   static final int ALLEGRO_KEY_L     = 12;
   static final int ALLEGRO_KEY_M     = 13;
   static final int ALLEGRO_KEY_N     = 14;
   static final int ALLEGRO_KEY_O     = 15;
   static final int ALLEGRO_KEY_P     = 16;
   static final int ALLEGRO_KEY_Q     = 17;
   static final int ALLEGRO_KEY_R     = 18;
   static final int ALLEGRO_KEY_S     = 19;
   static final int ALLEGRO_KEY_T     = 20;
   static final int ALLEGRO_KEY_U     = 21;
   static final int ALLEGRO_KEY_V     = 22;
   static final int ALLEGRO_KEY_W     = 23;
   static final int ALLEGRO_KEY_X     = 24;
   static final int ALLEGRO_KEY_Y     = 25;
   static final int ALLEGRO_KEY_Z     = 26;
   
   static final int ALLEGRO_KEY_0     = 27;
   static final int ALLEGRO_KEY_1     = 28;
   static final int ALLEGRO_KEY_2     = 29;
   static final int ALLEGRO_KEY_3     = 30;
   static final int ALLEGRO_KEY_4     = 31;
   static final int ALLEGRO_KEY_5     = 32;
   static final int ALLEGRO_KEY_6     = 33;
   static final int ALLEGRO_KEY_7     = 34;
   static final int ALLEGRO_KEY_8     = 35;
   static final int ALLEGRO_KEY_9     = 36;

   static final int ALLEGRO_KEY_PAD_0    = 37;
   static final int ALLEGRO_KEY_PAD_1    = 38;
   static final int ALLEGRO_KEY_PAD_2    = 39;
   static final int ALLEGRO_KEY_PAD_3    = 40;
   static final int ALLEGRO_KEY_PAD_4    = 41;
   static final int ALLEGRO_KEY_PAD_5    = 42;
   static final int ALLEGRO_KEY_PAD_6    = 43;
   static final int ALLEGRO_KEY_PAD_7    = 44;
   static final int ALLEGRO_KEY_PAD_8    = 45;
   static final int ALLEGRO_KEY_PAD_9    = 46;

   static final int ALLEGRO_KEY_F1    = 47;
   static final int ALLEGRO_KEY_F2    = 48;
   static final int ALLEGRO_KEY_F3    = 49;
   static final int ALLEGRO_KEY_F4    = 50;
   static final int ALLEGRO_KEY_F5    = 51;
   static final int ALLEGRO_KEY_F6    = 52;
   static final int ALLEGRO_KEY_F7    = 53;
   static final int ALLEGRO_KEY_F8    = 54;
   static final int ALLEGRO_KEY_F9    = 55;
   static final int ALLEGRO_KEY_F10   = 56;
   static final int ALLEGRO_KEY_F11   = 57;
   static final int ALLEGRO_KEY_F12   = 58;

   static final int ALLEGRO_KEY_ESCAPE     = 59;
   static final int ALLEGRO_KEY_TILDE      = 60;
   static final int ALLEGRO_KEY_MINUS      = 61;
   static final int ALLEGRO_KEY_EQUALS     = 62;
   static final int ALLEGRO_KEY_BACKSPACE  = 63;
   static final int ALLEGRO_KEY_TAB        = 64;
   static final int ALLEGRO_KEY_OPENBRACE  = 65;
   static final int ALLEGRO_KEY_CLOSEBRACE = 66;
   static final int ALLEGRO_KEY_ENTER      = 67;
   static final int ALLEGRO_KEY_SEMICOLON  = 68;
   static final int ALLEGRO_KEY_QUOTE      = 69;
   static final int ALLEGRO_KEY_BACKSLASH  = 70;
   static final int ALLEGRO_KEY_BACKSLASH2 = 71; /* DirectInput calls this DIK_OEM_102: "< > | on UK/Germany keyboards" */
   static final int ALLEGRO_KEY_COMMA      = 72;
   static final int ALLEGRO_KEY_FULLSTOP   = 73;
   static final int ALLEGRO_KEY_SLASH      = 74;
   static final int ALLEGRO_KEY_SPACE      = 75;

   static final int ALLEGRO_KEY_INSERT   = 76;
   static final int ALLEGRO_KEY_DELETE   = 77;
   static final int ALLEGRO_KEY_HOME     = 78;
   static final int ALLEGRO_KEY_END      = 79;
   static final int ALLEGRO_KEY_PGUP     = 80;
   static final int ALLEGRO_KEY_PGDN     = 81;
   static final int ALLEGRO_KEY_LEFT     = 82;
   static final int ALLEGRO_KEY_RIGHT    = 83;
   static final int ALLEGRO_KEY_UP       = 84;
   static final int ALLEGRO_KEY_DOWN     = 85;

   static final int ALLEGRO_KEY_PAD_SLASH    = 86;
   static final int ALLEGRO_KEY_PAD_ASTERISK = 87;
   static final int ALLEGRO_KEY_PAD_MINUS    = 88;
   static final int ALLEGRO_KEY_PAD_PLUS     = 89;
   static final int ALLEGRO_KEY_PAD_DELETE   = 90;
   static final int ALLEGRO_KEY_PAD_ENTER    = 91;

   static final int ALLEGRO_KEY_PRINTSCREEN = 92;
   static final int ALLEGRO_KEY_PAUSE       = 93;

   static final int ALLEGRO_KEY_ABNT_C1    = 94;
   static final int ALLEGRO_KEY_YEN        = 95;
   static final int ALLEGRO_KEY_KANA       = 96;
   static final int ALLEGRO_KEY_CONVERT    = 97;
   static final int ALLEGRO_KEY_NOCONVERT  = 98;
   static final int ALLEGRO_KEY_AT         = 99;
   static final int ALLEGRO_KEY_CIRCUMFLEX = 100;
   static final int ALLEGRO_KEY_COLON2     = 101;
   static final int ALLEGRO_KEY_KANJI      = 102;

   static final int ALLEGRO_KEY_PAD_EQUALS = 103;   /* MacOS X */
   static final int ALLEGRO_KEY_BACKQUOTE  = 104;   /* MacOS X */
   static final int ALLEGRO_KEY_SEMICOLON2 = 105;   /* MacOS X -- TODO: ask lillo what this should be */
   static final int ALLEGRO_KEY_COMMAND    = 106;   /* MacOS X */
   static final int ALLEGRO_KEY_BACK       = 107;
   static final int ALLEGRO_KEY_UNKNOWN    = 108;

      /* All codes up to before ALLEGRO_KEY_MODIFIERS can be freely
      * assignedas additional unknown keys, like various multimedia
      * and application keys keyboards may have.
      */

   static final int ALLEGRO_KEY_MODIFIERS = 215;

   static final int ALLEGRO_KEY_LSHIFT     = 215;
   static final int ALLEGRO_KEY_RSHIFT     = 216;
   static final int ALLEGRO_KEY_LCTRL      = 217;
   static final int ALLEGRO_KEY_RCTRL      = 218;
   static final int ALLEGRO_KEY_ALT        = 219;
   static final int ALLEGRO_KEY_ALTGR      = 220;
   static final int ALLEGRO_KEY_LWIN       = 221;
   static final int ALLEGRO_KEY_RWIN       = 222;
   static final int ALLEGRO_KEY_MENU       = 223;
   static final int ALLEGRO_KEY_SCROLLLOCK = 224;
   static final int ALLEGRO_KEY_NUMLOCK    = 225;
   static final int ALLEGRO_KEY_CAPSLOCK   = 226;
   
   static final int ALLEGRO_KEY_MAX = 227;
   
   private static int[] keyMap = {
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_UNKNOWN
      ALLEGRO_KEY_LEFT,       // KeyEvent.KEYCODE_SOFT_LEFT
      ALLEGRO_KEY_RIGHT,      // KeyEvent.KEYCODE_SOFT_RIGHT
      ALLEGRO_KEY_HOME,       // KeyEvent.KEYCODE_HOME
      ALLEGRO_KEY_BACK,    // KeyEvent.KEYCODE_BACK
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_CALL
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_ENDCALL
      ALLEGRO_KEY_0,          // KeyEvent.KEYCODE_0
      ALLEGRO_KEY_1,          // KeyEvent.KEYCODE_1
      ALLEGRO_KEY_2,          // KeyEvent.KEYCODE_2
      ALLEGRO_KEY_3,          // KeyEvent.KEYCODE_3
      ALLEGRO_KEY_4,          // KeyEvent.KEYCODE_4
      ALLEGRO_KEY_5,          // KeyEvent.KEYCODE_5 
      ALLEGRO_KEY_6,          // KeyEvent.KEYCODE_6 
      ALLEGRO_KEY_7,          // KeyEvent.KEYCODE_7 
      ALLEGRO_KEY_8,          // KeyEvent.KEYCODE_8 
      ALLEGRO_KEY_9,          // KeyEvent.KEYCODE_9 
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_STAR
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_POUND
      ALLEGRO_KEY_UP,         // KeyEvent.KEYCODE_DPAD_UP
      ALLEGRO_KEY_DOWN,       // KeyEvent.KEYCODE_DPAD_DOWN
      ALLEGRO_KEY_LEFT,       // KeyEvent.KEYCODE_DPAD_LEFT
      ALLEGRO_KEY_RIGHT,      // KeyEvent.KEYCODE_DPAD_RIGHT
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_DPAD_CENTER
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_VOLUME_UP
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_VOLUME_DOWN
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_POWER
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_CAMERA
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_CLEAR
      ALLEGRO_KEY_A,          // KeyEvent.KEYCODE_A
      ALLEGRO_KEY_B,          // KeyEvent.KEYCODE_B
      ALLEGRO_KEY_C,          // KeyEvent.KEYCODE_B
      ALLEGRO_KEY_D,          // KeyEvent.KEYCODE_D
      ALLEGRO_KEY_E,          // KeyEvent.KEYCODE_E
      ALLEGRO_KEY_F,          // KeyEvent.KEYCODE_F
      ALLEGRO_KEY_G,          // KeyEvent.KEYCODE_G
      ALLEGRO_KEY_H,          // KeyEvent.KEYCODE_H
      ALLEGRO_KEY_I,          // KeyEvent.KEYCODE_I
      ALLEGRO_KEY_J,          // KeyEvent.KEYCODE_J
      ALLEGRO_KEY_K,          // KeyEvent.KEYCODE_K 
      ALLEGRO_KEY_L,          // KeyEvent.KEYCODE_L
      ALLEGRO_KEY_M,          // KeyEvent.KEYCODE_M
      ALLEGRO_KEY_N,          // KeyEvent.KEYCODE_N
      ALLEGRO_KEY_O,          // KeyEvent.KEYCODE_O
      ALLEGRO_KEY_P,          // KeyEvent.KEYCODE_P
      ALLEGRO_KEY_Q,          // KeyEvent.KEYCODE_Q
      ALLEGRO_KEY_R,          // KeyEvent.KEYCODE_R
      ALLEGRO_KEY_S,          // KeyEvent.KEYCODE_S
      ALLEGRO_KEY_T,          // KeyEvent.KEYCODE_T
      ALLEGRO_KEY_U,          // KeyEvent.KEYCODE_U
      ALLEGRO_KEY_V,          // KeyEvent.KEYCODE_V
      ALLEGRO_KEY_W,          // KeyEvent.KEYCODE_W
      ALLEGRO_KEY_X,          // KeyEvent.KEYCODE_X
      ALLEGRO_KEY_Y,          // KeyEvent.KEYCODE_Y
      ALLEGRO_KEY_Z,          // KeyEvent.KEYCODE_Z
      ALLEGRO_KEY_COMMA,      // KeyEvent.KEYCODE_COMMA 
      ALLEGRO_KEY_FULLSTOP,     // KeyEvent.KEYCODE_PERIOD 
      ALLEGRO_KEY_ALT,        // KeyEvent.KEYCODE_ALT_LEFT
      ALLEGRO_KEY_ALTGR,      // KeyEvent.KEYCODE_ALT_RIGHT
      ALLEGRO_KEY_LSHIFT,     // KeyEvent.KEYCODE_SHIFT_LEFT
      ALLEGRO_KEY_RSHIFT,     // KeyEvent.KEYCODE_SHIFT_RIGHT
      ALLEGRO_KEY_TAB,        // KeyEvent.KEYCODE_TAB
      ALLEGRO_KEY_SPACE,      // KeyEvent.KEYCODE_SPACE
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_SYM
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_EXPLORER
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_ENVELOPE
      ALLEGRO_KEY_ENTER,      // KeyEvent.KEYCODE_ENTER
      ALLEGRO_KEY_DELETE,     // KeyEvent.KEYCODE_DEL
      ALLEGRO_KEY_TILDE,      // KeyEvent.KEYCODE_GRAVE
      ALLEGRO_KEY_MINUS,      // KeyEvent.KEYCODE_MINUS
      ALLEGRO_KEY_EQUALS,     // KeyEvent.KEYCODE_EQUALS
      ALLEGRO_KEY_OPENBRACE,  // KeyEvent.KEYCODE_LEFT_BRACKET
      ALLEGRO_KEY_CLOSEBRACE, // KeyEvent.KEYCODE_RIGHT_BRACKET
      ALLEGRO_KEY_BACKSLASH,  // KeyEvent.KEYCODE_BACKSLASH
      ALLEGRO_KEY_SEMICOLON,  // KeyEvent.KEYCODE_SEMICOLON
      ALLEGRO_KEY_QUOTE,      // KeyEvent.KEYCODE_APOSTROPHY
      ALLEGRO_KEY_SLASH,      // KeyEvent.KEYCODE_SLASH
      ALLEGRO_KEY_AT,         // KeyEvent.KEYCODE_AT
      ALLEGRO_KEY_NUMLOCK,    // KeyEvent.KEYCODE_NUM
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_HEADSETHOOK
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_FOCUS
      ALLEGRO_KEY_PAD_PLUS,   // KeyEvent.KEYCODE_PLUS
      ALLEGRO_KEY_MENU,       // KeyEvent.KEYCODE_MENU
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_NOTIFICATION
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_SEARCH
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_MEDIA_STOP
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_MEDIA_NEXT
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_MEDIA_PREVIOUS
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_MEDIA_REWIND
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_MEDIA_FAST_FORWARD
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_MUTE
      ALLEGRO_KEY_PGUP,       // KeyEvent.KEYCODE_PAGE_UP
      ALLEGRO_KEY_PGDN,       // KeyEvent.KEYCODE_PAGE_DOWN
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_PICTSYMBOLS
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_SWITCH_CHARSET
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_BUTTON_A
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_BUTTON_B
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_BUTTON_C
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_BUTTON_X
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_BUTTON_Y
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_BUTTON_Z
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_BUTTON_L1
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_BUTTON_R1
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_BUTTON_L2
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_BUTTON_R2
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_THUMBL
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_THUMBR
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_START
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_SELECT
      ALLEGRO_KEY_UNKNOWN,    // KeyEvent.KEYCODE_MODE
   };
   
   final int ALLEGRO_EVENT_TOUCH_BEGIN  = 50;
   final int ALLEGRO_EVENT_TOUCH_END    = 51;
   final int ALLEGRO_EVENT_TOUCH_MOVE   = 52;
   final int ALLEGRO_EVENT_TOUCH_CANCEL = 5;
   
   /** native functions we call */
   public native void nativeOnCreate();
   public native void nativeOnDestroy();
   public native void nativeOnChange(int format, int width, int height);
   public native void nativeOnKeyDown(int key);
   public native void nativeOnKeyUp(int key);
   public native void nativeOnTouch(int id, int action, float x, float y, boolean primary);
   
   /** functions that native code calls */
   
   private int[]       egl_Version = { 0, 0 };
   private EGLContext  egl_Context;
   private EGLSurface  egl_Surface;
   private EGLDisplay  egl_Display;
   private int         egl_numConfigs = 0;
   private EGLConfig[] egl_Config;
   
   public boolean egl_Init()
   {      
      Log.d("AllegroSurface", "egl_Init");
      EGL10 egl = (EGL10)EGLContext.getEGL();

      EGLDisplay dpy = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
      
      if(!egl.eglInitialize(dpy, egl_Version)) {
         Log.d("AllegroSurface", "egl_Init fail");
         return false;
      }
      
      egl_Display = dpy;
      
      egl_Config = new EGLConfig[100];
      int[] num_config = { 0 };
      
      if(!egl.eglGetConfigs(egl_Display, egl_Config, 100, num_config))
         return false;
      
      egl_numConfigs = num_config[0];
      
      Log.d("AllegroSurface", "egl_Init end");
      return true;
   }
   
   public int egl_getMajorVersion() { return egl_Version[0]; }
   public int egl_getMinorVersion() { return egl_Version[1]; }
   public int egl_getNumConfigs()   { return egl_numConfigs; }
   
   public int egl_getConfigAttrib(int conf, int attr)
   {
      EGL10 egl = (EGL10)EGLContext.getEGL();
      
      int[] value = { 0 };
      if(!egl.eglGetConfigAttrib(egl_Display, egl_Config[conf], attr, value))
         return -1;
      
      return value[0];
   }
   
   public boolean egl_createContext(int conf)
   {
      Log.d("AllegroSurface", "egl_createContext");
      EGL10 egl = (EGL10)EGLContext.getEGL();
      
      EGLContext ctx = egl.eglCreateContext(egl_Display, egl_Config[conf], EGL10.EGL_NO_CONTEXT, null);
      if(ctx == EGL10.EGL_NO_CONTEXT) {
         Log.d("AllegroSurface", "egl_createContext no context");
         return false;
      }
      
      egl_Context = ctx;
      
      Log.d("AllegroSurface", "egl_createContext end");
      return true;
   }
   
   public void egl_destroyContext()
   {
      EGL10 egl = (EGL10)EGLContext.getEGL();
      Log.d("AllegroSurface", "destroying egl_Context");
      egl.eglDestroyContext(egl_Display, egl_Context);
      egl_Context = EGL10.EGL_NO_CONTEXT;
   }
   
   public boolean egl_createSurface(int conf)
   {
      EGL10 egl = (EGL10)EGLContext.getEGL();
      EGLSurface surface = egl.eglCreateWindowSurface(egl_Display, egl_Config[conf], this, null);
      if(surface == EGL10.EGL_NO_SURFACE) {
         Log.d("AllegroSurface", "egl_createSurface can't create surface");
         return false;
      }
      
      if(!egl.eglMakeCurrent(egl_Display, surface, surface, egl_Context)) {
         egl.eglDestroySurface(egl_Display, surface);
         Log.d("AllegroSurface", "egl_createSurface can't make current");
         return false;
      }
      
      egl_Surface = surface;
      
      Log.d("AllegroSurface", "created new surface: " + surface);
      
      return true;
   }
   
   public void egl_destroySurface()
   {
      EGL10 egl = (EGL10)EGLContext.getEGL();
      if(!egl.eglMakeCurrent(egl_Display, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT)) {
         Log.d("AllegroSurface", "could not clear current context");
      }
      
      Log.d("AllegroSurface", "destroying egl_Surface");
      egl.eglDestroySurface(egl_Display, egl_Surface);
      egl_Surface = EGL10.EGL_NO_SURFACE;
   }
   
   public void egl_clearCurrent()
   {
      Log.d("AllegroSurface", "egl_clearCurrent");
      EGL10 egl = (EGL10)EGLContext.getEGL();
      if(!egl.eglMakeCurrent(egl_Display, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT)) {
         Log.d("AllegroSurface", "could not clear current context");
      }
      Log.d("AllegroSurface", "egl_clearCurrent done");
   }
   
   public void egl_makeCurrent()
   {
      EGL10 egl = (EGL10)EGLContext.getEGL();
      if(!egl.eglMakeCurrent(egl_Display, egl_Surface, egl_Surface, egl_Context)) {
      //   egl.eglDestroySurface(egl_Display, surface);
      //   egl.eglTerminate(egl_Display);
      //   egl_Display = null;
         Log.d("AllegroSurface", "can't make thread current: ");
         
         switch(egl.eglGetError()) {
            case EGL10.EGL_BAD_DISPLAY:
               Log.d("AllegroSurface", "Bad Display");
               break;
            case EGL10.EGL_NOT_INITIALIZED:
               Log.d("AllegroSurface", "Not Initialized");
               break;
            case EGL10.EGL_BAD_SURFACE:
               Log.d("AllegroSurface", "Bad Surface");
               break;
            case EGL10.EGL_BAD_CONTEXT:
               Log.d("AllegroSurface", "Bad Context");
               break;
            case EGL10.EGL_BAD_MATCH:
               Log.d("AllegroSurface", "Bad Match");
               break;
            case EGL10.EGL_BAD_ACCESS:
               Log.d("AllegroSurface", "Bad Access");
               break;
            case EGL10.EGL_BAD_NATIVE_PIXMAP:
               Log.d("AllegroSurface", "Bad Native Pixmap");
               break;
            case EGL10.EGL_BAD_NATIVE_WINDOW:
               Log.d("AllegroSurface", "Bad Native Window");
               break;
            case EGL10.EGL_BAD_CURRENT_SURFACE:
               Log.d("AllegroSurface", "Bad Current Surface");
               break;
            case EGL10.EGL_BAD_ALLOC:
               Log.d("AllegroSurface", "Bad Alloc");
               break;
            
            default:
               Log.d("AllegroSurface", "unknown error");
         }
         
         return;
      }  
   }
   
   public void egl_SwapBuffers()
   {
      //try {
      //   Log.d("AllegroSurface", "posting SwapBuffers!");
      //   AllegroActivity.Self.postRunnable( new Runnable() {
      //      public void run() {
               try {
                  EGL10 egl = (EGL10)EGLContext.getEGL();
                  
                  egl.eglWaitNative(EGL10.EGL_NATIVE_RENDERABLE, null);

                  egl.eglWaitGL();

                  egl.eglSwapBuffers(egl_Display, egl_Surface);
                  switch(egl.eglGetError()) {
                     case EGL10.EGL_SUCCESS:
                        break; // things are ok
                     
                     case EGL10.EGL_BAD_DISPLAY:
                        Log.e("AllegroSurface", "SwapBuffers: Bad Display");
                        break;
                        
                     case EGL10.EGL_NOT_INITIALIZED:
                        Log.e("AllegroSurface", "SwapBuffers: display not initialized");
                        break;
                        
                     case EGL10.EGL_BAD_SURFACE:
                        Log.e("AllegroSurface", "SwapBuffers: Bad Surface: " + egl_Surface + " " + (egl_Surface == EGL10.EGL_NO_SURFACE));
                        break;
                        
                     case EGL11.EGL_CONTEXT_LOST:
                        Log.e("AllegroSurface", "SwapBuffers: Context Lost");
                        break;
                        
                     case EGL10.EGL_BAD_NATIVE_WINDOW:
                        Log.d("AllegroSurface", "SwapBuffers: Bad native window");
                        break;
                        
                     default:
                        Log.d("AllegroSurface", "Unhandled SwapBuffers Error");
                        break;
                  }
                  
               } catch(Exception x) {
                  Log.d("AllegroSurface", "inner exception: " + x.getMessage());
               }
      //      }
      //   } );
      //} catch(Exception x) {
      //   Log.d("AllegroSurface", "exception: " + x.getMessage());
      //}
   }
   
   /** main handlers */
   
   public AllegroSurface(Context context)
   {
      super(context);

        
      Log.d("AllegroSurface", "ctor");
      
      setFocusable(true);
      setFocusableInTouchMode(true);
      requestFocus();
      setOnKeyListener(this); 
      setOnTouchListener(this);   
      
      getHolder().addCallback(this); 
      
      Log.d("AllegroSurface", "ctor end");
   }

   public void surfaceCreated(SurfaceHolder holder)
   {
      Log.d("AllegroSurface", "surfaceCreated");
      nativeOnCreate();
      Log.d("AllegroSurface", "surfaceCreated end");
   }

   public void surfaceDestroyed(SurfaceHolder holder)
   {
      Log.d("AllegroSurface", "surfaceDestroyed");
      nativeOnDestroy();
      
      egl_destroySurface();
      egl_destroyContext();
      
      EGL10 egl = (EGL10)EGLContext.getEGL();
      egl.eglTerminate(egl_Display);
      egl_Display = null;
      
      Log.d("AllegroSurface", "surfaceDestroyed end");
   }

   public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
   {
      Log.d("AllegroSurface", "surfaceChanged");
      int allegro_fmt = ALLEGRO_PIXEL_FORMAT_RGB_565;
      
      switch(format) {
         case PixelFormat.RGBA_4444:
            allegro_fmt = ALLEGRO_PIXEL_FORMAT_RGBA_4444;
            break;
            
         case PixelFormat.RGBA_5551:
            allegro_fmt = ALLEGRO_PIXEL_FORMAT_RGBA_5551;
            break;
            
         case PixelFormat.RGBA_8888:
            allegro_fmt = ALLEGRO_PIXEL_FORMAT_RGBA_8888;
            break;
            
         case PixelFormat.RGB_565:
            allegro_fmt = ALLEGRO_PIXEL_FORMAT_RGB_565;
            break;
            
         case PixelFormat.RGB_888:
            allegro_fmt = ALLEGRO_PIXEL_FORMAT_RGB_888;
            break;
            
         default:
            Log.v("AllegroSurface", "unkown pixel format " + format);
            break;
      }
      
      nativeOnChange(allegro_fmt, width, height);

      Log.d("AllegroSurface", "surfaceChanged end");
   }
   
   /* unused */
   public void onDraw(Canvas canvas) { }
   
   /* events */
   
   /* maybe dump a stacktrace and die, rather than loging, and ignoring errors */
   /* all this fancyness is so we work on as many versions of android as possible,
    * and gracefully degrade, rather than just outright failing */
   
   private boolean fieldExists(Object obj, String fieldName)
   {
      try {
         Class cls = obj.getClass();
         Field m = cls.getField(fieldName);
         return true;
      } catch(Exception x) {
         return false;
      }
   }
   
   private <T> T getField(Object obj, String field)
   {
      try {
         Class cls = obj.getClass();
         Field f = cls.getField(field);
         return (T)f.get(obj);
      }
      catch(NoSuchFieldException x) {
         Log.v("AllegroSurface", "field " + field + " not found in class " + obj.getClass().getCanonicalName());
         return null;
      }
      catch(IllegalArgumentException x) {
         Log.v("AllegroSurface", "this shouldn't be possible, but fetching " + field + " from class " + obj.getClass().getCanonicalName() + " failed with an illegal argument exception");
         return null;
      }
      catch(IllegalAccessException x) {
         Log.v("AllegroSurface", "field " + field + " is not accessible in class " + obj.getClass().getCanonicalName());
         return null;
      }
   }
   
   private boolean methodExists(Object obj, String methName)
   {
      try {
         Class cls = obj.getClass();
         Method m = cls.getMethod(methName);
         return true;
      } catch(Exception x) {
         return false;
      }
   }
   
   private <T> T callMethod(Object obj, String methName, Class [] types, Object... args)
   {
      try {
         Class cls = obj.getClass();
         Method m = cls.getMethod(methName, types);
         return (T)m.invoke(obj, args);
      }
      catch(NoSuchMethodException x) {
         Log.v("AllegroSurface", "method " + methName + " does not exist in class " + obj.getClass().getCanonicalName());
         return null;
      }
      catch(NullPointerException x) {
         Log.v("AllegroSurface", "can't invoke null method name");
         return null;
      }
      catch(SecurityException x) {
         Log.v("AllegroSurface", "method " + methName + " is not accessible in class " + obj.getClass().getCanonicalName());
         return null;
      }
      catch(IllegalAccessException x)
      {
         Log.v("AllegroSurface", "method " + methName + " is not accessible in class " + obj.getClass().getCanonicalName());
         return null;
      }
      catch(InvocationTargetException x)
      {
         Log.v("AllegroSurface", "method " + methName + " threw an exception :(");
         return null;
      }
   }
   
   public boolean onKey(View  v, int keyCode, KeyEvent event)
   {
      if (event.getAction() == KeyEvent.ACTION_DOWN) {
         nativeOnKeyDown(keyMap[keyCode]);
         return true;
      }
      else if (event.getAction() == KeyEvent.ACTION_UP) {
         nativeOnKeyUp(keyMap[keyCode]);
         return true;
      }
         
      return false;
   }
   
   // FIXME: pull out android version detection into the setup
   // and just check some flags here, rather than checking for
   // the existance of the fields and methods over and over
   public boolean onTouch(View v, MotionEvent event)
   {
      //Log.d("AllegroSurface", "onTouch");
      int action = 0;
      int pointer_id = 0;
      
      Class[] no_args = new Class[0];
      Class[] int_arg = new Class[1];
      int_arg[0] = int.class;
      
      if(methodExists(event, "getActionMasked")) { // android-8 / 2.2.x
         action = this.<Integer>callMethod(event, "getActionMasked", no_args);
         int ptr_idx = this.<Integer>callMethod(event, "getActionIndex", no_args);
         pointer_id = this.<Integer>callMethod(event, "getPointerId", int_arg, ptr_idx);
      } else {
         int mask = 0xff;
         int raw_action = event.getAction();
         
         if(fieldExists(event, "ACTION_MASK")) { // android-5 / 2.0
            mask = this.<Integer>getField(event, "ACTION_MASK");
            action = raw_action & mask;
            
            int ptr_id_mask = this.<Integer>getField(event, "ACTION_POINTER_ID_MASK");
            int ptr_id_shift = this.<Integer>getField(event, "ACTION_POINTER_ID_SHIFT");
            
            pointer_id = (raw_action & ptr_id_mask) >> ptr_id_shift;
         }
         else { // android-4 / 1.6
            /* no ACTION_MASK? no multi touch, no pointer_id, */
            action = raw_action;
         }
      }
      
      boolean primary = false;
      
      if(action == MotionEvent.ACTION_DOWN) {
         primary = true;
         action = ALLEGRO_EVENT_TOUCH_BEGIN;
      }
      else if(action == MotionEvent.ACTION_UP) {
         primary = true;
         action = ALLEGRO_EVENT_TOUCH_END;
      }
      else if(action == MotionEvent.ACTION_MOVE) {
         action = ALLEGRO_EVENT_TOUCH_MOVE;
      }
      else if(action == MotionEvent.ACTION_CANCEL) {
         action = ALLEGRO_EVENT_TOUCH_CANCEL;
      }
      // android-5 / 2.0
      else if(fieldExists(event, "ACTION_POINTER_DOWN") &&
         action == this.<Integer>getField(event, "ACTION_POINTER_DOWN"))
      {
         action = ALLEGRO_EVENT_TOUCH_BEGIN;
      }
      else if(fieldExists(event, "ACTION_POINTER_UP") &&
         action == this.<Integer>getField(event, "ACTION_POINTER_UP"))
      {
         action = ALLEGRO_EVENT_TOUCH_END;
      }
      else {
         Log.v("AllegroSurface", "unknown MotionEvent type: " + action);
         Log.d("AllegroSurface", "onTouch endf");
         return false;
      }
      
      if(methodExists(event, "getPointerCount")) { // android-5 / 2.0
         int pointer_count = this.<Integer>callMethod(event, "getPointerCount", no_args);
         for(int i = 0; i < pointer_count; i++) {
            float x = this.<Float>callMethod(event, "getX", int_arg, i);
            float y = this.<Float>callMethod(event, "getY", int_arg, i);
            int  id = this.<Integer>callMethod(event, "getPointerId", int_arg, i);
            
            /* not entirely sure we need to report move events for non primary touches here
             * but examples I've see say that the ACTION_[POINTER_][UP|DOWN]
             * report all touches and they can change between the last MOVE
             * and the UP|DOWN event */
            if(id == pointer_id) {
               nativeOnTouch(id, action, x, y, primary);
            } else {
               nativeOnTouch(id, ALLEGRO_EVENT_TOUCH_MOVE, x, y, primary);
            }
         }
      } else {
         nativeOnTouch(pointer_id, action, event.getX(), event.getY(), primary);
      }

      //Log.d("AllegroSurface", "onTouch end");
      return true;
   }

   

}

/* vim: set sts=3 sw=3 et: */