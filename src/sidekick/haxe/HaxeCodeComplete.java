package sidekick.haxe;

import org.gjt.sp.jedit.EditAction;
import org.gjt.sp.jedit.View;

public class HaxeCodeComplete extends EditAction
{
    public static String NAME = "haxeCodeComplete";
    public HaxeCodeComplete()
    {
        super(NAME);
    }
    @Override
    public void invoke (View view)
    {
    // TODO Auto-generated method stub
        System.out.println("Invoked haxe code completion");
    }

//    /**
//     * Create a new AutoComplete that starts working for the given buffer if the buffer has none.
//     * If the buffer already has an AutoComplete, it's only returned.
//     */
//    public static AutoComplete createAutoCompleteAction( final Buffer buffer )
//    {
//        Pattern filter = PreferencesManager.getPreferencesManager().getFilenameFilterPattern();
//        if (filter != null) {
//            String path = buffer.getPath();
//            boolean match = filter.matcher(path).matches();
//            if (match == PreferencesManager.getPreferencesManager().isExclusionFilter())
//                return null;
//        }
//        AutoComplete autoComplete = getAutoCompleteOfBuffer(buffer);
//        if(autoComplete == null)
//        {
//            autoComplete = new AutoComplete(buffer);
//            bufferToAutoComplete.put(buffer, autoComplete);
//        }
//        else
//        {
//            if( autoComplete.getBuffer() == null || autoComplete.getBuffer() != buffer )
//            { autoComplete.attach(buffer); }
//        }
//        return autoComplete;
//    }

}
