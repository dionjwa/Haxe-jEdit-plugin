package sidekick;


import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

public class CodeCellRenderer implements ListCellRenderer
{
	private static Icon greenCircleIcon = new CircleIcon(new Color(20,202,59));
	private static Icon yellowDiamondIcon = new DiamondIcon2(new Color(235, 225, 48));
	private static Icon greyCircleIcon = new CircleIcon(Color.gray);
	private Icon classImageIcon;

	protected DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

	public CodeCellRenderer()
	{
		ClassLoader cldr = this.getClass().getClassLoader();
		java.net.URL imageURL = cldr.getResource("icons/classicon.png");
		classImageIcon = new ImageIcon(imageURL);
	}

	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
	{
//		Font theFont = null;
//		Color theForeground = null;
//		Icon theIcon = null;
//		String theText = null;

		JLabel renderer = (JLabel) defaultRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

		renderer.setMinimumSize(new Dimension(1, 16));

		if (value instanceof CodeCompletion)
		{
			CodeCompletion cc = (CodeCompletion)value;

			switch(cc.getType())
			{
				case METHOD:
				{
					renderer.setIcon(greenCircleIcon);
					renderer.setText(cc.getCodeCompletionString());
					break;
				}

				case FIELD:
				{
					renderer.setIcon(yellowDiamondIcon);
					renderer.setText(cc.getCodeCompletionString());
					break;
				}

				case VARIABLE:
				{
					renderer.setIcon(greyCircleIcon);
					renderer.setText(cc.getCodeCompletionString());
					break;
				}

				case CLASS:
				{
					renderer.setIcon(classImageIcon);
					renderer.setText(cc.getCodeCompletionString());
					break;
				}

				default :
				{
					renderer.setText(cc.getCodeCompletionString());
					break;
				}
			}

		}
		else
		{
			renderer.setText(value.toString());
		}
//		else if(value instanceof CodeCompletionField)
//		{
//			renderer.setIcon(new DiamondIcon2(yellowColor));
//			renderer.setText(((CodeCompletionField)value).getCodeCompletionString());
//		}
//		else if(value instanceof CodeCompletionVariable)
//		{
//			renderer.setIcon(new CircleIcon(Color.gray));
//			renderer.setText(((CodeCompletionVariable)value).getCodeCompletionString());
//		}
//		else
//		{
//			renderer.setIcon(new CircleIcon(Color.blue));
//			renderer.setText(value.toString());
//		}
//


//		if(value instanceof CodeCompletionMethod)
//		{
//			renderer.setIcon(new CircleIcon(Color.red));
////			Object values[] = (Object[]) value;
////			theFont = (Font) values[0];
////			theForeground = (Color) values[1];
////			theIcon = (Icon) values[2];
////			theText = (String) values[3];
//		}
//		else
//		{
//			theFont = list.getFont();
//			theForeground = list.getForeground();
//			theText = "";
//		}
//		if(!isSelected)
//		{
//			renderer.setForeground(theForeground);
//		}
//		if(theIcon != null)
//		{
//			renderer.setIcon(theIcon);
//		}
//		renderer.setText(theText);
//		renderer.setFont(theFont);
		return renderer;
	}
}

