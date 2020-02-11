// FractalDemo.java

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
import javax.swing.JLabel;
import java.util.ArrayList;
import java.util.NoSuchElementException;

public class FractalDemo extends JFrame
{
   private JComboBox fractalBox;
   private FractalPanel fractalPanel;

   public FractalDemo()
   {
      super("Fractal Demo");

      fractalBox = new JComboBox(Fractals.getNames());
      fractalBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
               if(event.getStateChange() == ItemEvent.SELECTED) {
                  //JOptionPane.showMessageDialog( null, String.format(
                  //      "Item #%d", fractalBox.getSelectedIndex() ) );
                  int id = fractalBox.getSelectedIndex();
                  Fractal currentFractal = Fractals.getFractal(id);
                  fractalPanel.setImage(currentFractal.getImage());
                  fractalPanel.repaint();
               }
            }
         }
      );
      add(fractalBox, BorderLayout.NORTH);

      fractalPanel = new FractalPanel();
      fractalPanel.setImage(Fractals.getFractal(0).getImage());
      add(fractalPanel, BorderLayout.CENTER);

      JLabel copyright =
         new JLabel("2018-2020 (c) Горобейко В.С.", JLabel.CENTER);
      add(copyright, BorderLayout.SOUTH);

      setResizable(false);
      pack();
   } // end constructor FractalDemo

   public static void main( String[] args )
   {
      JFrame app = new FractalDemo();
      app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      app.setVisible(true);
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
   public static final int MANDELBROT = 2;
   public static final int NEWTON = 3;

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

      fractals.add( new GeometricFractalInfo( "Кривая Кох", "60",
            "F:F-F++F-F" ) );
      fractals.add( new GeometricFractalInfo( "Снежинка Кох", "60",
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
      fractals.add( new FractalInfo( "Множество Мандельброта",
            FractalInfo.MANDELBROT ) );
      fractals.add( new FractalInfo( "Крест Ньютона",
            FractalInfo.NEWTON ) );
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
      if(info.getType() == FractalInfo.GEOMETRIC) {
         return new GeometricFractal( 400, 400, (GeometricFractalInfo) info );
      } else if(info.getType() == FractalInfo.MANDELBROT) {
         return new MandelbrotFractal(400, 400, -2.2, 1, -1.2, 1.2);
      } else if(info.getType() == FractalInfo.NEWTON) {
         return new NewtonFractal(400, 400);
      }
      return new DummyFractal();
   }
} // end class Fractals

class Complex
{
   private double Re, Im;

   public Complex() {
      this(0,0);
   }

   public Complex(Complex c) {
      this(c.Re, c.Im);
   }

   public Complex(double re, double im) {
      Re = re;
      Im = im;
   }

   public void add(Complex c) {
      Re += c.Re;
      Im += c.Im;
   }

   public void subtract(Complex c) {
      Re -= c.Re;
      Im -= c.Im;
   }

   public void multiply(Complex c) {
      double re = Re*c.Re-Im*c.Im;
      double im = Re*c.Im+Im*c.Re;
      Re = re;
      Im = im;
   }

   public void divide(Complex c) {
      double k = c.Re*c.Re+c.Im*c.Im;
      if( k == 0 )
         throw new ArithmeticException( "can't divide by zero" );
      double re = Re*c.Re+Im*c.Im;
      double im = Im*c.Re-Re*c.Im;
      Re = re / k;
      Im = im / k;
   }

   public double sqrmod() {
      return Re*Re+Im*Im;
   }

   public static Complex valueOf(double x) {
      return new Complex(x, 0);
   }
} // end class Complex

class MandelbrotFractal extends Fractal
{
   private static final int MaxIterations = 511;
   private BufferedImage image;

   public MandelbrotFractal(int w, int h,
         double minX, double maxX, double minY, double maxY) {
      image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
      double stepX = (maxX - minX) / (double) w;
      double stepY = (maxY - minY) / (double) h;
      double y = minY;
      for( int j=0; j<h; j++ )
      {
         double x = minX;
         for( int i=0; i<w; i++ )
         {
            image.setRGB(i, j, getColor(x, y));
            x += stepX;
         } // end inner for
         y += stepY;
      } // end outer for
   }// end constructor

   @Override
   public BufferedImage getImage()
   {
      return image;
   } // end method getImage

   private int getColor( double x, double y ) {
      // C = (x+yi)
      Complex C = new Complex( x, y );
      // Z = (0+0i)
      Complex Z = Complex.valueOf( 0 );
      int i;
      for( i = 0; i < MaxIterations; i++ ) {
         // 0: (0+0i)(0+0i) = (0+0i)
         // 1: (x+yi)(x+yi) = xx+xyi+xyi-yy = (xx-yy, 2xyi)
         Z.multiply( Z ); // ineffective computations
         // 0: (0+0i)+(x+yi) = (x+yi)
         // 1: (xx-yy+x, (2xy+y)i)
         Z.add( C );
         // 0: x*x+y*y > 4
         if( Z.sqrmod() > 4 )
            break;
      }
      return 8*(MaxIterations-i);
   } // end method getColor
} // end class MandelbrotFractal

class NewtonFractal extends Fractal
{
   private BufferedImage image;

   public NewtonFractal(int w, int h) {
      image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
      // minX = -1, maxX = 1, dx = 1-(-1)=2
      // minY = -1, maxY = 1, dy = 1-(-1)=2
      double dx = ((double) 2) / 400, x0 = -1;
      double dy = -((double) 2) / 400, y0 = 1;

      double x, y;
      int i, j;
      for(i=0, y=y0; i<400; i++, y+=dy)
         for(j=0, x=x0; j<400; j++, x+=dx)
            image.setRGB(j, i, getColor(x, y));
   }// end constructor

   @Override
   public BufferedImage getImage()
   {
      return image;
   } // end method getImage

   private int getColor( double x, double y ) {
      Complex Z = new Complex( x, y );
      int i;
      for( i = 0; i < 511; i++ ) {
         Z = nextValue( Z );
         if( checkCondition( Z ) )
            break;
      }
      return 8*(511-i);
   } // end method getColor

   // Zk = ( 3 * Z^4 + 1 ) / ( 4 * Z^3 )
   private Complex nextValue( Complex Z )
   {
      Complex z4 = new Complex( Z );
      z4.multiply( z4 );

         Complex z3 = new Complex( z4 );
         z3.multiply( Z );
         z3.multiply( Complex.valueOf( 4 ) );

      z4.multiply( z4 );
      z4.multiply( Complex.valueOf( 3 ) );
      z4.add( Complex.valueOf( 1 ) );
      try {
         z4.divide( z3 );
      } catch( ArithmeticException e ) {
         z4 = new Complex( 65536, 0 );
      }
      return z4;
   } // end method nextValue

   // while |Z^4 - 1| ^ 2 > 0.001
   private boolean checkCondition( Complex Z )
   {
      Complex z = new Complex( Z );
      z.multiply( z );
      z.multiply( z );
      z.subtract( Complex.valueOf( 1 ) );
      double x = z.sqrmod();
      return ( x > 0.001 )? false : true ;
   } // end method checkCondition
} // end class NewtonFractal
