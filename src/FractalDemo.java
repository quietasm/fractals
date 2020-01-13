import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import java.util.ArrayList;
import java.util.NoSuchElementException;

public class FractalDemo extends JFrame
{
   private JComboBox fractalBox;
   private FractalPanel fractalPanel;

   public FractalDemo()
   {
      super( "Fractal Demo" );

      fractalBox = new JComboBox( Fractals.getNames() );
      fractalBox.addItemListener( new ItemListener() {
            public void itemStateChanged( ItemEvent event ) {
               if( event.getStateChange() == ItemEvent.SELECTED ) {
                  //JOptionPane.showMessageDialog( null, String.format(
                  //      "Item #%d", fractalBox.getSelectedIndex() ) );
                  int id = fractalBox.getSelectedIndex();
                  Fractal currentFractal = Fractals.getFractal( id );
                  fractalPanel.setImage( currentFractal.getImage() );
                  fractalPanel.repaint();
               }
            }
         }
      );
      add( fractalBox, BorderLayout.NORTH );

      fractalPanel = new FractalPanel();
      fractalPanel.setImage( Fractals.getFractal( 0 ).getImage() );
      add( fractalPanel, BorderLayout.CENTER );

      pack();
   } // end constructor FractalDemo

   public static void main( String[] args )
   {
      JFrame app = new FractalDemo();
      app.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
      app.setVisible( true );
   } // end main
} // end class FractalDemo

class FractalPanel extends JPanel
{
   private BufferedImage image; 

   public void setImage( BufferedImage image )
   {
      this.image = image;
   } // end method setImage

   @Override
   public Dimension getMinimumSize()
   {
      if( image != null )
         return new Dimension( image.getWidth(), image.getHeight() );
      else
         return new Dimension( 400, 400 );
   } // end method getMinimumSize

   @Override
   public Dimension getPreferredSize()
   {
      return getMinimumSize();
   } // end method getPreferredSize

   @Override
   public void paintComponent( Graphics g )
   {
      super.paintComponent( g );
      if( image != null )
      {
         g.drawImage(image, 0, 0, null);
      } // end if
   } // end method paintComponent
} // end class FractalPanel

class FractalInfo
{
   public static final int NONE = 0;
   public static final int GEOMETRIC = 1;

   private String fractalName;
   private int fractalType;

   public FractalInfo( String name, int type )
   {
      fractalName = name;
      fractalType = type;
   } // end constructor

   public FractalInfo( String name )
   {
      this( name, NONE );
   } // end constructor

   public String getName()
   {
      return fractalName;
   } // end method getName

   public int getType()
   {
      return fractalType;
   } // end method getType
} // end class FractalInfo

class GeometricFractalInfo extends FractalInfo
{
   private float angle;
   private Rule[] rules;

   public GeometricFractalInfo( String name, String angle, String rules )
   {
      super( name, FractalInfo.GEOMETRIC );

      this.angle = Float.parseFloat( angle );

      ArrayList< Rule > tmp = new ArrayList< Rule >();
      String[] parts = rules.split( ";" );
      for( String part : parts )
      {
         String[] tokens = part.split( ":" );
         tmp.add( new Rule( tokens[ 0 ].trim(), tokens[1].trim() ) );
      }
      this.rules = tmp.toArray( new Rule[ tmp.size() ] );
   } // end constructor

   public float getAngle()
   {
      return angle;
   } // end method getAngle

   public Rule getRoot()
   {
      return rules[ 0 ];
   } // end method getRoot

   public Rule getRule( String name )
   {
      for( int i = 0; i < rules.length; i++ )
         if( rules[ i ].getName().equals( name ) )
            return rules[ i ];
      return null;
   } // end method getRule
} // end class GeometricFractalInfo

class Rule
{
   private String name;
   private String value;

   public Rule( String x, String y ) {
      name = x;
      value = y;
   }

   public String getName() { return name; }
   public String getValue() { return value; }
} // end class Rule

abstract class Fractal
{
   public abstract BufferedImage getImage();
}

class DummyFractal extends Fractal
{
   @Override
   public BufferedImage getImage() { return null; }
} // end class DummyFractal

class GeometricFractal extends Fractal
{
   private GeometricFractalInfo fractalParams;
   private Turtle turtle;
   private BufferedImage image;

   public GeometricFractal( int w, int h, GeometricFractalInfo params )
   {
      fractalParams = params;
      turtle = new Turtle( 4 );

      String lastPattern = null;
      TurtleInfo lastInfo = null;
      for( int level = 1; level < 100; level++ )
      {
         String pattern = expand( fractalParams,
               fractalParams.getRoot().getValue(), level );
         TurtleInfo info = new TurtleInfo( w, h );
         turtle.clearState();
         execute( fractalParams, pattern, info );
         if( info.getWidth() >= w || info.getHeight() >= h )
            break;
         lastPattern = pattern;
         lastInfo = info;
      } // end for

      TurtlePainter painter = new TurtlePainter( w, h );
      turtle.clearState();
      turtle.setX( lastInfo.getX() );
      turtle.setY( lastInfo.getY() );
      execute( fractalParams, lastPattern, painter );
      image = painter.getImage();
   } // end constructor

   @Override
   public BufferedImage getImage()
   {
      return image;
   } // end method getImage

   private String expand( GeometricFractalInfo params,
         String pattern, int level ) throws NoSuchElementException
   {
      StringBuilder result = new StringBuilder();
      for( int i = 0; i < pattern.length(); i++ )
      {
         char x = pattern.charAt( i );
         if( x == '+' || x == '-' || x == '[' || x == ']' )
            result.append( x );
         else
         {
            Rule rule = params.getRule( String.format( "%c", x ) );
            if( rule != null && level > 0 )
               result.append( expand( params, rule.getValue(), level-1 ) );
            else if( x == 'F' || x == 'f' ) // command here
               result.append( x );
            else if( rule == null )
               throw new NoSuchElementException( String.format(
                     "Rule '%c' not found", x ) );
         }
      } // end for
      return result.toString();
   } // end method expand

   private void execute( GeometricFractalInfo params,
         String pattern, Line line )
   {
      for( int i = 0; i < pattern.length(); i++ )
      {
         switch( pattern.charAt( i ) )
         {
            case '+': turtle.rotate( params.getAngle() ); break;
            case '-': turtle.rotate( -params.getAngle() ); break;
            case '[': turtle.pushState(); break;
            case ']': turtle.popState(); break;
            case 'F': turtle.forward( line ); break;
            case 'f': turtle.forward( null ); break;
         }
      } // end for
   } // end method execute
} // end class GeometricFractal

abstract class Line
{
   public abstract void draw( int x1, int y1, int x2, int y2 );
}

class TurtleInfo extends Line
{
   private int minX, minY, maxX, maxY, width, height;

   public TurtleInfo( int w, int h )
   {
      minX = minY = 0;
      maxX = maxY = 0;
      width = w;
      height = h;
   } // end constructor TurtleInfo

   @Override
   public void draw( int x1, int y1, int x2, int y2 )
   {
      if( x2 < minX ) { minX = x2; } else if( x2 > maxX ) { maxX = x2; }
      if( y2 < minY ) { minY = y2; } else if( y2 > maxY ) { maxY = y2; }
   } // end method draw

   public int getWidth() { return maxX - minX; }
   public int getHeight() { return maxY - minY; }

   public int getX() { return ( width - ( minX + maxX ) ) / 2; }
   public int getY() { return ( height - ( minY + maxY ) ) / 2; }

   @Override
   public String toString()
   {
      return String.format( "minX = %d\nminY = %d\nmaxX = %d\nmaxY = %d",
         minX, minY, maxX, maxY );
   } // end method toString
} // end class TurtleInfo

class TurtlePainter extends Line
{
   BufferedImage img;
   Graphics2D g;

   public TurtlePainter( int w, int h )
   {
      img = new BufferedImage( w, h, BufferedImage.TYPE_INT_RGB );
      g = img.createGraphics();
      g.setColor( Color.GREEN );
   } // end constructor TurtlePainter

   public BufferedImage getImage() { return img; }

   @Override
   public void draw( int x1, int y1, int x2, int y2 )
   {
      g.drawLine( x1, y1, x2, y2 );
   } // end method draw
} // end class TurtlePainter

class Turtle {
   private final static int MAXIMUM_STATE = 50;
   private final static int X = 0;
   private final static int Y = 1;
   private final static int ANGLE = 2;

   private int scale;
   private float state[][];
   private int top;

   public Turtle( int scale )
   {
      this.scale = scale;
      state = new float[3][MAXIMUM_STATE];
      clearState();
   }

   public Turtle()
   {
      this( 5 );
   }

   public void clearState()
   {
      top = 0;
      setX(0);
      setY(0);
      setAngle(0);
   }

   // get- and set-methods

   public void setX( float x ) { state[X][top] = x; }
   public void setY( float y ) { state[Y][top] = y; }
   public void setAngle( float a ) { state[ANGLE][top] = a; }

   public float getX() { return state[X][top]; }
   public float getY() { return state[Y][top]; }
   public float getAngle() { return state[ANGLE][top]; }

   // Turtle commands

   // [
   public void pushState()
   {
      ++top;
      state[X][top] = state[X][top-1];
      state[Y][top] = state[Y][top-1];
      state[ANGLE][top] = state[ANGLE][top-1];
   }

   // ]
   public void popState() { --top; }

   // +, -
   public void rotate( float delta ) { state[ANGLE][top] += delta; }

   // F, f
   public void forward( Line line )
   {
      float a = (float)Math.PI / 180 * state[ANGLE][top];
      float x = state[X][top] + (float)Math.cos( a ) * scale;
      float y = state[Y][top] + (float)Math.sin( a ) * scale;
      if( line != null )
      {
         line.draw( (int)state[X][top], (int)state[Y][top], (int)x, (int)y );
      }
      state[X][top] = x;
      state[Y][top] = y;
   }
} // end class Turtle

class Fractals
{
   private static ArrayList< FractalInfo > fractals;

   static {
      fractals = new ArrayList< FractalInfo >();

      fractals.add( new GeometricFractalInfo( "Кривая Коха", "60",
            "F:F-F++F-F" ) );
      fractals.add( new GeometricFractalInfo( "Снежинка Коха", "60",
            ":F++F++F;F:F-F++F-F" ) );
      fractals.add( new GeometricFractalInfo( "Фрактал 3", "120",
            ":F+F+F;F:F-F+F" ) );
      fractals.add( new GeometricFractalInfo( "Фрактал 4", "90",
            ":F+F+F+F;F:FF+F++F+F" ) );
      fractals.add( new GeometricFractalInfo( "Кривая Дракона", "90",
            ":FX;X:X+YF+;Y:-FX-Y" ) );
      fractals.add( new GeometricFractalInfo( "Кривая Госпера", "60",
            ":XF;X:X+YF++YF-FX--FXFX-YF+;Y:-FX+YFYF++YF+FX--FX-Y" ) );
      fractals.add( new GeometricFractalInfo( "Кривая Серпинского", "90",
            ":F+XF+F+XF;X:XF-F+F-XF+F+XF-F+F-X" ) );
      fractals.add( new GeometricFractalInfo( "Кривая Гильберта", "90",
            ":X;X:-YF+XFX+FY-;Y:+XF-YFY-FX+" ) );
      fractals.add( new GeometricFractalInfo( "Фрактал 9", "90",
            ":F+F+F+F;F:FF+F+F+F+FF" ) );
      fractals.add( new GeometricFractalInfo( "Фрактал 10", "90",
            ":F+F+F+F;F:F+F-F-FF+F+F-F" ) );
      fractals.add( new GeometricFractalInfo( "Фрактал 11", "90",
            ":F+F+F+F;F:F+F-F-FFF+F+F-F" ) );
      fractals.add( new GeometricFractalInfo( "Фрактал 12", "90",
            ":F+F+F+F;F:F-FF+FF+F+F-F-FF+F+F-F-FF-FF+F" ) );
      fractals.add( new GeometricFractalInfo( "Фрактал 13", "90",
            ":F;F:F-F+F+F-F" ) );
      fractals.add( new GeometricFractalInfo( "Фрактал 14", "60",
            ":YF;X:YF+XF+Y;Y:XF-YF-X" ) );
      fractals.add( new GeometricFractalInfo( "Фрактал 15", "90",
            ":F+F+F+F;F:F+F-F+F+F" ) );
      fractals.add( new GeometricFractalInfo( "Фрактал 16", "90",
            ":F+F+F+F;F:FF+F+F+F+F+F-F" ) );
      fractals.add( new GeometricFractalInfo( "Куст 1", "25.7",
            ":Y;X:X[-FFF][+FFF]FX;Y:YFX[+Y][-Y]" ) );
      fractals.add( new GeometricFractalInfo( "Куст 2", "22.5",
            ":F;F:FF+[+F-F-F]-[-F+F+F]" ) );
      fractals.add( new GeometricFractalInfo( "Куст 3", "36",
            ":F;F:F[+FF][-FF]F[-F][+F]F" ) );
      fractals.add( new GeometricFractalInfo( "Куст 4", "20",
            ":X;F:FF;X:F[+X]F[-X]+X" ) );
      fractals.add( new GeometricFractalInfo( "Куст 5", "90",
            ":F-F-F-F;F:F-F+F+F-F" ) );
      fractals.add( new GeometricFractalInfo( "Сорняк", "25.7",
            ":F;F:F[+F]F[-F]F" ) );
      fractals.add( new GeometricFractalInfo( "Фрактал 23", "60",
            ":F;F:FXF;X:[-F+F+F]+F-F-F+" ) );
      fractals.add( new GeometricFractalInfo( "Треугольник Серпинского", "60",
            ":FXF--FF--FF;F:FF;X:--FXF++FXF++FXF--" ) );
      fractals.add( new GeometricFractalInfo( "Ковёр Серпинского", "90",
            ":F;F:FFF[+FFF+FFF+FFF]" ) );
      //fractals.add( new FractalInfo( "Множество Мандельброта" ) );
      //fractals.add( new FractalInfo( "Крест Ньютона" ) );
   }

   public static String[] getNames()
   {
      String[] names = new String[ fractals.size() ];
      for( int i = 0; i < fractals.size(); i++ )
         names[ i ] = fractals.get( i ).getName();
      return names;
   }

   public static Fractal getFractal( int id )
   {
      FractalInfo info = fractals.get( id );
      if( info.getType() == FractalInfo.GEOMETRIC )
      {
         return new GeometricFractal( 400, 400, (GeometricFractalInfo) info );
      }
      return new DummyFractal();
   }
} // end class Fractals
