package com.psddev.cms.tool;

import com.psddev.cms.db.Schedule;

import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.AbstractFilter;
import com.psddev.dari.util.Settings;
import com.psddev.dari.util.Task;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;

/** Triggers scheduled events for publishing. */
public class ScheduleFilter extends AbstractFilter {

    public static final String HOST_SETTING = "brightspot/scheduleHost";
    public static final String SCHEDULE_THREAD_NAME = "ScheduleFilter";

    public final Task scheduler = new Task() {

        @Override
        public void doTask() {
            if (ObjectType.getInstance(Schedule.class.getName()) == null) {
                return;
            }

            String host = Settings.get(String.class, HOST_SETTING);
            boolean run;

            if (host == null) {
                run = true;

            } else {
                try {
                    String hostAddress = InetAddress.getByName(host).getHostAddress();
                    String localAddress = InetAddress.getLocalHost().getHostAddress();
                    run = hostAddress != null && hostAddress.equals(localAddress);

                } catch (UnknownHostException error) {
                    run = false;
                }
            }

            if (!run) {
                return;
            }

            Thread.currentThread().setName(SCHEDULE_THREAD_NAME);

            for (Schedule schedule : Query
                    .from(Schedule.class)
                    .sortAscending("triggerDate")
                    .master()
                    .noCache()
                    .resolveInvisible()
                    .iterable(0)) {

                try {
                    schedule.trigger();

                } catch (Exception ex1) {
                    try {
                        StringWriter writer = new StringWriter();
                        ex1.printStackTrace(new PrintWriter(writer));
                        schedule.getState().put("cms.lastException", writer.toString());
                        schedule.save();
                    } catch (Exception ex2) {
                        // Ignore any error caused by trying to save the error
                        // information to the schedule itself.
                    }
                }
            }
        }
    };

    // --- AbstractFilter support ---

    @Override
    protected void doInit() {
        scheduler.scheduleWithFixedDelay(60.0, 60.0);
    }

    @Override
    protected void doDestroy() {
        scheduler.stop();
    }
}
