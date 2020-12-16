package nineBoxPuzzle;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

final class Pair
{
	public int f,s;
	public Pair(int x,int y)
	{
		this.f = x;
		this.s = y;
	}
}

final class Puzzle extends JPanel
{	
	private JFrame FRAME;
	private JPanel PANEL;
	private int POSX,POSY,ABSX,ABSY,MOVES,BEST;
	private final int OFFSET,WINDOW_WIDTH,WINDOW_HEIGHT,IMG_LEN,N;
	private String PLAYER_NAME;
	private boolean WIN_STATUS;
	private final double ANIMATION_SPEED;
	private double C;
	private Pair[] NUM;
	private Pair PREV,HOLE;
	private ArrayList<BufferedImage> IMGS = new ArrayList<BufferedImage>();
	private ArrayList<Integer> RAN = new ArrayList<Integer>();
	private File SCORESHEET;
	private Thread LOOPER;
	private Clip CLIP;
	private AudioInputStream AIS;
	
	private Puzzle(JFrame F)
	{
		super();
		this.FRAME = F;
		this.PANEL = this;
		this.SCORESHEET = new File("HighScore.dat");
		this.C = 0;
		this.MOVES = 0;
		this.BEST = this.MOVES;
		this.PLAYER_NAME = new String("DEFAULT");
		this.WIN_STATUS = false;
		this.N = 3;
		this.NUM = new Pair[this.N*this.N];
		this.ANIMATION_SPEED = 0.003;
		this.IMG_LEN = 64;
		this.WINDOW_WIDTH = 400;
		this.WINDOW_HEIGHT = 400;
		this.OFFSET = (this.WINDOW_WIDTH-this.N*this.IMG_LEN)/2;
		this.PREV = new Pair(-1,-1);
		this.HOLE = new Pair(this.N-1,this.N-1);
		for(int i=0;i<(this.N*this.N);i++)
		{
			this.NUM[i] = new Pair(i%this.N,i/this.N);
		}
		this.initScoreSheet();
		this.initRan();
		this.eventHandler(F);
		this.prepareGUI();
		this.LOOPER = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				JOptionPane.showMessageDialog(PANEL, "ABOUT\n\nThe 9-box puzzle is a sliding puzzle which consists of 3x3 grid of numbered\nsquares with one square missing. The squares are jumbled when the puzzle\nstart and the goal of this game is to unjumble the squares by only sliding a\nsquare into the empty space.\n\n", "9 Box-Puzzle", JOptionPane.PLAIN_MESSAGE);
				while(true)
				{
					revalidate();
					repaint();
					FRAME.setVisible(true);
				}
			}
		});
		this.LOOPER.start();
		this.playSound();
	}
	
	private void initScoreSheet()
	{
		if(!this.SCORESHEET.exists())
		{
			try 
			{
				this.SCORESHEET.createNewFile();
			}
			catch (IOException e) 
			{
				e.printStackTrace();
			}
			this.setHighScore();
		}
		else
		{
			String[] TEMP = new String[2];
			TEMP = this.getHighScore().split(": ");
			this.PLAYER_NAME = TEMP[0];
			this.BEST = Integer.parseInt(TEMP[1]);
		}
	}
	
	private void setHighScore()
	{
		try 
		{
			FileWriter WRITER = new FileWriter(this.SCORESHEET);
			BufferedWriter BWRITER = new BufferedWriter(WRITER);
			BWRITER.write(this.PLAYER_NAME+": "+this.MOVES);
			BWRITER.close();
			WRITER.close();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	private String getHighScore()
	{
		String STR = "";
		try
		{
			FileReader READER = new FileReader(this.SCORESHEET);
			BufferedReader BREADER = new BufferedReader(READER);
			STR = new String(BREADER.readLine());
			BREADER.close();
			READER.close();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		return STR;
	}
	
	private void initRan()
	{
		this.RAN.clear();
		for(int i=0;i<(this.N*this.N-1);i++)
		{
			this.RAN.add(i);
		}
		Collections.shuffle(this.RAN);
		while(!this.checkRAN())
		{
			Collections.shuffle(this.RAN);
		}
		this.RAN.add(this.N*this.N-1);
	}
	
	private boolean checkRAN()
	{
		int ans = 0;
		for(int i=0;i<(this.N*this.N-2);i++)
		{
			for(int j=i+1;j<(this.N*this.N-1);j++)
			{
				if(this.RAN.get(j)<this.RAN.get(i))
				{
					ans++;
				}
			}
		}
		return ans%2 == 0;
	}
	
	private boolean checkWin()
	{
		boolean flag = true;
		for(int i=0;i<(this.N*this.N);i++)
		{
			if(this.RAN.get(i) != i)
			{
				flag = false;
				break;
			}
		}
		return flag;
	}
	
	private void restartGame()
	{
		this.MOVES = 0;
		initRan();
	}
	
	private void prepareGUI()
	{
		try
		{
			for(int i=0;i<(this.N*this.N-1);i++)
			{
				this.IMGS.add(ImageIO.read(new File("src/Resources/"+(i+1)+".png")));
			}
			this.IMGS.add(null);
		}
		catch(IOException e)
		{
			System.exit(0);
		}
		this.setPreferredSize(new Dimension(this.WINDOW_WIDTH,this.WINDOW_HEIGHT));
	}
	
	private void makeMove(int x,int y)
	{
		if(x >= this.N || y >= this.N || x < 0 || y < 0)
		{
			return;
		}
		this.ABSX = Math.abs(this.HOLE.f - x);
		this.ABSY = Math.abs(this.HOLE.s - y);
		if((this.ABSX == 0 || this.ABSX == 1) && (this.ABSY == 1 || this.ABSY == 0) && this.ABSX != this.ABSY)
		{
			this.MOVES++;
			int TEMP = this.HOLE.s*this.N+this.HOLE.f;
			this.RAN.set(TEMP, this.RAN.get(y*this.N+x));
			this.RAN.set(y*this.N+x, this.N*this.N-1);
			this.PREV.f = this.HOLE.f;
			this.PREV.s = this.HOLE.s;
			this.HOLE.f = x;
			this.HOLE.s = y;
		}
	}
	
	private void eventHandler(JFrame F)
	{
		F.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent windowEvent)
			{
				Thread TEMP_THREAD = new Thread(new Runnable()
				{
					@Override
					public void run()
					{
						JLabel JL = new JLabel("Are you sure you want to quit the game?");
						Font FF = new Font("Times New Roman",Font.BOLD,20);
						JL.setFont(FF);
						int RES = JOptionPane.showConfirmDialog(F, JL, "Warning!", JOptionPane.YES_NO_OPTION);
						if(RES == JOptionPane.YES_OPTION)
						{
							FRAME.dispose();
							CLIP.close();
							System.exit(0);
						}
					}
				});
				TEMP_THREAD.start();
			}
		});
		this.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent mouseEvent)
			{
				if(C == 0 && !WIN_STATUS)
				{
					POSX = mouseEvent.getX();
					POSY = mouseEvent.getY();
					POSX -= OFFSET;
					POSY -= OFFSET;
					if(POSX >= 0 && POSY >= 0)
					makeMove(POSX/IMG_LEN,POSY/IMG_LEN);
				}
			}
		});
	}
	
	private void playSound()
	{
		if(CLIP != null)
		CLIP.start();
		try
		{
			AIS = AudioSystem.getAudioInputStream(new File("src/Resources/clack.wav"));
			CLIP = AudioSystem.getClip();
			CLIP.open(AIS);
		}
		catch (LineUnavailableException | UnsupportedAudioFileException | IOException e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		Graphics2D G = (Graphics2D)g;
		Font F = new Font("Times New Roman",Font.BOLD,30);
		FontMetrics METRICS = G.getFontMetrics(F);
		G.setFont(F);
		G.setBackground(Color.WHITE);
		Color CO = new Color(255,100,15);
		G.setColor(CO);
		BasicStroke S = new BasicStroke(5);
		G.setStroke(S);
		G.drawRect(this.OFFSET, this.OFFSET, this.WINDOW_WIDTH-2*this.OFFSET, this.WINDOW_HEIGHT-2*this.OFFSET);
		G.setStroke(S);
		G.drawRect(this.OFFSET-10, this.OFFSET-10, this.WINDOW_WIDTH-2*this.OFFSET+20, this.WINDOW_HEIGHT-2*this.OFFSET+20);
		String STR = new String("Moves: "+this.MOVES);
		String STR1 = new String("Best Score: "+this.BEST+" by "+this.PLAYER_NAME);
		int loc = (this.WINDOW_WIDTH - METRICS.stringWidth(STR))/2;
		G.setColor(Color.BLACK);
		G.drawString(STR, loc, 35);
		loc = (this.WINDOW_WIDTH - METRICS.stringWidth(STR1))/2;
		G.setColor(Color.BLACK);
		G.drawString(STR1, loc, 70);
		int u = this.HOLE.f,v = this.HOLE.s;
		int TEMP = this.PREV.s*this.N+this.PREV.f;
		for(int i=0;i<(this.N*this.N);i++)
		{
			if(this.IMGS.get(this.RAN.get(i)) != null && i != TEMP)
			{
				int locx,locy;
				locx = this.OFFSET+this.IMG_LEN*this.NUM[i].f;
				locy = this.OFFSET+this.IMG_LEN*this.NUM[i].s;
				G.drawImage(this.IMGS.get(this.RAN.get(i)), null, locx, locy);
			}
		}
		if(this.PREV.f == this.HOLE.f)
		{
			if(this.PREV.s > this.HOLE.s)
			{
				G.translate(this.OFFSET+this.IMG_LEN*u, this.OFFSET+this.IMG_LEN*(v+this.C));
				G.drawImage(this.IMGS.get(this.RAN.get(TEMP)), null, 0, 0);
				G.translate(-(this.OFFSET+this.IMG_LEN*u), -(this.OFFSET+this.IMG_LEN*(v+this.C)));
				this.C += this.ANIMATION_SPEED;
			}
			else
			{
				G.translate(this.OFFSET+this.IMG_LEN*u, this.OFFSET+this.IMG_LEN*(v+this.C));
				G.drawImage(this.IMGS.get(this.RAN.get(TEMP)), null, 0, 0);
				G.translate(-(this.OFFSET+this.IMG_LEN*u), -(this.OFFSET+this.IMG_LEN*(v+this.C)));
				this.C -= this.ANIMATION_SPEED;
			}
			if(this.C > 1 || this.C < -1)
			{
				this.C = 0;
				this.PREV.f = -1;
				this.PREV.s = -1;
				if(this.checkWin())
				{
					Thread TEMP_THREAD = new Thread(new Runnable()
					{
						@Override
						public void run()
						{
							JLabel JL;
							Font FF = new Font("Times New Roman",Font.BOLD,20);
							if(MOVES < BEST || BEST == 0)
							{
								JL = new JLabel("Congratulations! You have set a new best score. Please write your name.");
								JL.setFont(FF);
								PLAYER_NAME = JOptionPane.showInputDialog(PANEL, JL);
								setHighScore();
								BEST = MOVES;
							}
							JL = new JLabel("You Won in "+MOVES+" Moves! Do you want to restart the game?");
							JL.setFont(FF);
							int RES = JOptionPane.showConfirmDialog(PANEL, JL, "9 Box-Puzzle", JOptionPane.YES_NO_OPTION);
							if(RES == JOptionPane.YES_OPTION)
							{
								restartGame();
							}
							else
							{
								WIN_STATUS = true;
							}
						}
					});
					TEMP_THREAD.start();
				}
				this.playSound();
			}
		}
		if(this.PREV.s == this.HOLE.s)
		{
			if(this.PREV.f > this.HOLE.f)
			{
				G.translate(this.OFFSET+this.IMG_LEN*(u+this.C), this.OFFSET+this.IMG_LEN*v);
				G.drawImage(this.IMGS.get(this.RAN.get(TEMP)), null, 0, 0);
				G.translate(-(this.OFFSET+this.IMG_LEN*(u+this.C)), -(this.OFFSET+this.IMG_LEN*v));
				this.C += this.ANIMATION_SPEED;
			}
			else
			{
				G.translate(this.OFFSET+this.IMG_LEN*(u+this.C), this.OFFSET+this.IMG_LEN*v);
				G.drawImage(this.IMGS.get(this.RAN.get(TEMP)), null, 0, 0);
				G.translate(-(this.OFFSET+this.IMG_LEN*(u+this.C)), -(this.OFFSET+this.IMG_LEN*v));
				this.C -= this.ANIMATION_SPEED;
			}
			if(this.C > 1 || this.C < -1)
			{
				this.C = 0;
				this.PREV.f = -1;
				this.PREV.s = -1;
				if(this.checkWin())
				{
					Thread TEMP_THREAD = new Thread(new Runnable()
					{
						@Override
						public void run()
						{
							JLabel JL;
							Font FF = new Font("Times New Roman",Font.BOLD,20);
							if(MOVES < BEST || BEST == 0)
							{
								JL = new JLabel("Congratulations! You have set a new best score. Please write your name.");
								JL.setFont(FF);
								PLAYER_NAME = JOptionPane.showInputDialog(PANEL, JL);
								setHighScore();
								BEST = MOVES;
							}
							JL = new JLabel("You Won in "+MOVES+" Moves! Do you want to restart the game?");
							JL.setFont(FF);
							int RES = JOptionPane.showConfirmDialog(PANEL, JL, "9 Box-Puzzle", JOptionPane.YES_NO_OPTION);
							if(RES == JOptionPane.YES_OPTION)
							{
								restartGame();
							}
							else
							{
								WIN_STATUS = true;
							}
						}
					});
					TEMP_THREAD.start();
				}
				this.playSound();
			}
		}
		G.dispose();
		g.dispose();
	}
	
	public static void main(String[] args)
	{
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				JFrame F = new JFrame("9-Box Puzzle");
				Puzzle Game = new Puzzle(F);
				F.add(Game);
				F.pack();
				F.setLocationRelativeTo(null);
			}
		});
	}
}