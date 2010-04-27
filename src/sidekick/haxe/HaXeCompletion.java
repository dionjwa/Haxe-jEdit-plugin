package sidekick.haxe;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.ListCellRenderer;

import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.textarea.Selection;

import sidekick.CodeCellRenderer;
import sidekick.CodeCompletion;
import sidekick.CodeCompletionMethod;
import sidekick.CodeCompletionVariable;
import sidekick.SideKickCompletion;

public class HaXeCompletion extends SideKickCompletion
{

    private List<CodeCompletion> _codeCompletions;

    /**
     * Typing can shorten the list of completions.
     */
    private List<CodeCompletion> _codeCompletionsAfterTyping;
    private Map<String, CodeCompletionVariable> _localvars;

    public HaXeCompletion(View view, String text, Map<String, CodeCompletionVariable> localvars, List<? extends CodeCompletion>... variables)
    {
        super(view, text);
        _localvars = localvars;
        _codeCompletions = new LinkedList<CodeCompletion>();
        for (List< ? extends CodeCompletion> list : variables)
        {
            _codeCompletions.addAll(list);
        }
        Collections.sort(_codeCompletions);
        _codeCompletionsAfterTyping = new LinkedList<CodeCompletion>();
        for (CodeCompletion cc : _codeCompletions)
        {
            _codeCompletionsAfterTyping.add(cc);
        }
    }

    @Override
    public Object get(int index)
    {
        return _codeCompletionsAfterTyping.get(index);
    }

    @Override
    public String getCompletionDescription(int index)
    {
        return _codeCompletionsAfterTyping.get(index).getCodeCompletionString();
    }

    @Override
    public ListCellRenderer getRenderer()
    {
        return new CodeCellRenderer();
    }

    @Override
    public int getTokenLength()
    {
        return super.getTokenLength();
    }

    @Override
    public boolean handleKeystroke(int selectedIndex, char keyChar)
    {
        if(keyChar == '\t' || keyChar == '\n')
        //if(SideKickActions.acceptChars.indexOf(keyChar) > -1)
        {
            insert(selectedIndex);
//          if(SideKickActions.insertChars.indexOf(keyChar) > -1)
//              textArea.userInput(keyChar);
            return false;
        }
        else
        {
            if(keyChar == '\b' )
            {
//              if(typedKeys.length() > 0)
//                  typedKeys = typedKeys.substring(0, typedKeys.length() - 1);
                return false;
            }
            else if(String.valueOf(keyChar).matches("\\w"))
            {
//              typedKeys = typedKeys + keyChar;
                text += keyChar;
                textArea.userInput(keyChar);
            }

            _codeCompletionsAfterTyping.clear();

            for (CodeCompletion cc : _codeCompletions)
            {
                if(cc.getStringForInsertion().startsWith(text))
                    _codeCompletionsAfterTyping.add(cc);
            }

            return true;
        }

    }

    @Override
    public void insert(int index)
    {
        CodeCompletion cc = _codeCompletionsAfterTyping.get(index);
        String selected = cc.getStringForInsertion();

        int caret = textArea.getCaretPosition();
        int lineNumber = textArea.getCaretLine();
        if(text.length() > 0 && !text.endsWith("."))
        {
            Selection toReplace = new Selection.Range(caret-text.length(), caret);
            textArea.setSelection(toReplace);
            textArea.backspace();
        }
        caret = textArea.getCaretPosition();
//      Selection s = textArea.getSelectionAtOffset(caret);
//      int start = (s == null ? caret : s.getStart());
//      int end = (s == null ? caret : s.getEnd());
        JEditBuffer buffer = textArea.getBuffer();


        int finalSelectionIndexStart = -1;
        int finalSelectionIndexEnd = -1;
        try
        {
            buffer.beginCompoundEdit();
//          buffer.remove(start - text.length(),text.length());
//          buffer.insert(start - text.length(),selected);

            if(cc instanceof CodeCompletionMethod)
            {
                CodeCompletionMethod cm = (CodeCompletionMethod)cc;
                StringBuilder sb = new StringBuilder();
                sb.append(cm.name+"(");

                for (int i = 0; i < cm.arguments.size(); i++)
                {
                    String possiblearg = null;

                    if( i > 0)
                        sb.append( ", ");
                    if(possiblearg != null)
                    {
                        sb.append( possiblearg);
                    }
                    else
                    {
                        sb.append( cm.arguments.get(i));
                        //As this must be changed by the user,we can select it if it the first to select
                        if(finalSelectionIndexStart == -1)
                        {
                            finalSelectionIndexStart = caret + sb.length() - cm.arguments.get(i).length();
                            finalSelectionIndexEnd = caret + sb.length() ;
                        }
                    }
                }

                sb.append(")");
                buffer.insert(caret,sb.toString());
            }
            else
                buffer.insert(caret,selected);

            /*Set the selection to the first argument*/


        }
        finally
        {
            buffer.endCompoundEdit();
        }

        if(finalSelectionIndexEnd != -1)
        {
            textArea.setCaretPosition(finalSelectionIndexEnd);
            Selection.Range finalSelection = new Selection.Range(finalSelectionIndexStart, finalSelectionIndexEnd);
            textArea.setSelection(finalSelection);

        }

//      if(cc instanceof CodeCompletionMethod)
//      {
//
//
//          CodeCompletionMethod m = (CodeCompletionMethod)cc;
//          if(m.arguments.size() > 0)
//          {
//              textArea.setCaretPosition(caret + m.name.length() + 1);
//              Selection s = new Selection.Range(caret + m.name.length() + 1, caret + m.name.length() + 1 + m.arguments.get(0).length());
//              textArea.setSelection(s);
//          }
//          else
//              textArea.setCaretPosition(caret + m.name.length() + 2);
//      }
    }

    @Override
    public boolean isCompletionSelectable(int index)
    {
//      for (CodeCompletion cc : _codeCompletions)
//      {
//          if(cc.getType() == CodeCompletion.NULL_COMPLETION)
//              return false;
//      }
        return true;
    }


    @Override
    public int size()
    {
        return _codeCompletionsAfterTyping.size();
    }

    @Override
    public boolean updateInPlace(EditPane editPane, int caret)
    {
        return true;
    }


}
