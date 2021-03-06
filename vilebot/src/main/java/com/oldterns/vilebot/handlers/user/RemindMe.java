package com.oldterns.vilebot.handlers.user;

import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;
import net.engio.mbassy.listener.Handler;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oldterns.vilebot.util.BaseNick;

@HandlerContainer
public class RemindMe
{

    private static final String timeFormat = "(\\d+\\w*)";

    private static final Pattern remindMePattern = Pattern.compile( "^!remindme (.+) " + timeFormat );

    private final String INVALID_TYPE_ERROR =
        "The time type given is not valid (use d for day, m for month, s for second)";

    private final String NO_TYPE_ERROR = "There was no type given for the time (use d/m/s)";

    private final String TIME_TOO_LARGE_ERROR = "The value of time given is greater than the maximum Integer value";

    private final String TOO_MANY_REMINDERS_ERROR =
        "There is a limit of 10 reminders, please wait until one reminder ends to set a new one.";

    private final String TIME_IS_OKAY = "Given time input is okay";

    private String timeError = TIME_IS_OKAY;

    private static Map<String, Integer> userReminders = new HashMap<>();

    private final int MAX_REMINDERS = 10;

    @Handler
    public void doRemindMe( ReceivePrivmsg event )
    {
        String text = event.getText();
        Matcher matcher = remindMePattern.matcher( text );

        if ( matcher.matches() )
        {
            String message = matcher.group( 1 );
            String time = matcher.group( 2 );
            String creator = BaseNick.toBaseNick( event.getSender() );
            if ( !userReminders.containsKey( creator ) )
            {
                userReminders.put( creator, 0 );
            }
            Calendar timerTime = getTimerTime( time, creator );
            if ( timerTime == null )
            {
                event.replyPrivately( String.format( "The given time of %s is invalid. The cause is %s.", time,
                                                     timeError ) );
            }
            else
            {
                Timer timer = new Timer();
                timer.schedule( createTimerTask( event, message, creator ), timerTime.getTime() );
                event.replyPrivately( "Created reminder for " + timerTime.getTime() );
                int amountOfReminders = userReminders.get( creator );
                amountOfReminders++;
                userReminders.put( creator, amountOfReminders );
            }
        }
    }

    private TimerTask createTimerTask( final ReceivePrivmsg event, final String message, final String creator )
    {
        return new TimerTask()
        {
            @Override
            public void run()
            {
                event.replyPrivately( "This is your reminder that you should: " + message );
                int amountOfReminders = userReminders.get( creator );
                amountOfReminders--;
                userReminders.put( creator, amountOfReminders );
            }
        };
    }

    private Calendar getTimerTime( final String time, final String creator )
    {
        Calendar calendar = Calendar.getInstance();
        verifyTime( time, creator );
        if ( !timeError.equals( TIME_IS_OKAY ) )
        {
            return null;
        }
        Integer timeValue = new Integer( time.substring( 0, time.length() - 1 ) );
        switch ( time.substring( time.length() - 1 ) )
        {
            case "d":
                calendar.add( Calendar.DAY_OF_MONTH, timeValue );
            case "m":
                calendar.add( Calendar.MINUTE, timeValue );
            case "s":
                calendar.add( Calendar.SECOND, timeValue );
        }
        return calendar;
    }

    private void verifyTime( final String time, final String creator )
    {
        String type = time.substring( time.length() - 1 );
        if ( !type.equals( "d" ) && !type.equals( "m" ) && !type.equals( "s" ) )
        {
            if ( isNumeric( time ) )
            {
                timeError = NO_TYPE_ERROR;
            }
            else
            {
                timeError = INVALID_TYPE_ERROR;
            }
            return;
        }
        try
        {
            String givenTime = time.substring( 0, time.length() - 1 );
            new Integer( givenTime );
        }
        catch ( Exception e )
        {
            timeError = TIME_TOO_LARGE_ERROR;
            return;
        }
        if ( userReminders.get( creator ) == MAX_REMINDERS )
        {
            timeError = TOO_MANY_REMINDERS_ERROR;
            return;
        }
        timeError = TIME_IS_OKAY;
    }

    private boolean isNumeric( final String time )
    {
        try
        {
            new Integer( time );
        }
        catch ( Exception e )
        {
            return false;
        }
        return true;
    }
}
