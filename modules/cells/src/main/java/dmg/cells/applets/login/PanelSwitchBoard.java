package dmg.cells.applets.login ;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.CardLayout;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;


public class      PanelSwitchBoard
       extends    SshActionPanel
       implements ActionListener ,
                  ItemListener   {

   private static final long serialVersionUID = -3333694444209526122L;
   private CardLayout _cards;
   private Panel      _cardPanel;
   private Panel      _buttonPanel;
   private CheckboxGroup _checkGroup = new CheckboxGroup() ;
    private Font _font = new Font( "TimesRoman" , 0 , 24 ) ;
    private boolean _useBoxes;
    private int  _b = 14 ;
    @Override
    public Insets getInsets(){ return new Insets( _b , _b ,_b , _b ) ; }
    @Override
    public void paint( Graphics g ){
       Dimension d = getSize() ;
       int h = _b / 2 ;
       g.setColor( Color.white ) ;
       int [] xs = new int[4] ;
       int [] ys = new int[4] ;
       xs[0] = h           ; ys[0] = h ;
       xs[1] = d.width-h-1 ; ys[1] = h ;
       xs[2] = d.width-h-1 ; ys[2] = d.height - h - 1 ;
       xs[3] = h           ; ys[3] = d.height - h - 1 ;
       g.drawPolygon( xs , ys , xs.length  ) ;
    }
   public PanelSwitchBoard(){ this(false) ; }

   public PanelSwitchBoard( boolean useBoxes ){
      _useBoxes = useBoxes ;
      if( ! useBoxes ) {
          _PanelSwitchBoardButtons();
      } else {
          _PanelSwitchBoardBoxes();
      }
   }
   private void _PanelSwitchBoardBoxes(){
      setBackground( new Color( 190 , 190 , 190 ) ) ;
      setLayout( new BorderLayout() ) ;
      _cardPanel   = new Panel() ;
      _cardPanel.setLayout( _cards = new CardLayout()  ) ;

      BorderLayout bl = new BorderLayout() ;
      bl.setHgap(10) ;
      bl.setVgap(10) ;

      GridLayout gl = new GridLayout(0,1) ;
      gl.setHgap(10) ;
      gl.setVgap(10) ;

      Panel leftPanel = new Panel( bl ) ;


      _buttonPanel = new Panel(gl) ;

      Label label = new Label( "Panels" , Label.CENTER ) ;
      label.setFont( _font ) ;
      Button b    = new Button( "Back" ) ;
      b.addActionListener(this) ;
      b.setFont( _font ) ;

      Panel centerPanel = new Panel( new BorderLayout() ) ;
      centerPanel.add( _buttonPanel , "North" ) ;
      leftPanel.add( label , "North" ) ;
      leftPanel.add( centerPanel , "Center" ) ;
      leftPanel.add( b , "South" ) ;


//      Panel buttonFrame = new Panel( new BorderLayout() ) ;
//      buttonFrame.add( leftPanel , "North" ) ;

      super.add( leftPanel , "West" ) ;
      super.add( _cardPanel , "Center" ) ;
   }
   private void _PanelSwitchBoardButtons(){
      setLayout( new BorderLayout() ) ;
      _cardPanel   = new Panel() ;
      _cardPanel.setLayout( _cards = new CardLayout()  ) ;

      GridLayout gl = new GridLayout(0,1) ;
      gl.setHgap(10) ;
      gl.setVgap(10) ;
      _buttonPanel = new Panel(gl) ;

      Button b     = new Button( "Back" ) ;
      b.addActionListener(this) ;

      Label label = new Label( "Panels" , Label.CENTER ) ;
      label.setFont( _font ) ;

      _buttonPanel.add( label ) ;
      _buttonPanel.add( b ) ;

      Panel buttonFrame = new Panel( new BorderLayout() ) ;
      buttonFrame.add( _buttonPanel , "North" ) ;

      super.add( buttonFrame , "West" ) ;
      super.add( _cardPanel , "Center" ) ;
   }
   public void add( Component comp , String name ){
      _cardPanel.add( comp , name ) ;
      if( _useBoxes ){
        Checkbox box = new Checkbox( name , _checkGroup , false ) ;
        box.addItemListener( this ) ;
        _buttonPanel.add( box ) ;
      }else{
         Button b = new Button( name ) ;
         b.addActionListener( this ) ;
         _buttonPanel.add( b ) ;
      }
   }
   @Override
   public void itemStateChanged( ItemEvent event ){
      String command = ((Checkbox)event.getSource()).getLabel() ;
      _cards.show( _cardPanel , command ) ;
   }
   @Override
   public void actionPerformed( ActionEvent event ){
      String command = event.getActionCommand() ;
//      System.out.println( "Action : "+command ) ;
      if( command.equals( "Back" ) ){
         informActionListeners( "exit" ) ;
         return ;
      }
      _cards.show( _cardPanel , command ) ;
   }
}
