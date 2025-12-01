package com.example.lottos.organizer;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.widget.EditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * A helper class that simplifies the process of picking a date and a time.
 *
 * Role: This utility class encapsulates the logic for displaying a DatePickerDialog
 * followed immediately by a TimePickerDialog. Upon completion, it formats the
 * selected date and time into a "yyyy-MM-dd HH:mm" string and sets this value
 * as the text of a target EditText field. This abstracts away the boilerplate
 * code required for handling date/time picking from the Fragment or Activity.
 */
public class DateTimePickerHelper {

    private final Context context;

    /**
     * Constructs a new DateTimePickerHelper.
     *
     * @param context The Android context required to display the dialogs.
     */
    public DateTimePickerHelper(Context context) {
        this.context = context;
    }

    /**
     * Shows a sequence of a DatePickerDialog and a TimePickerDialog.
     * The method first prompts the user to select a date. Once a date is chosen,
     * it immediately prompts the user to select a time. The final combined
     * date and time are then formatted and applied to the provided EditText.
     *
     * @param target The EditText widget that will be populated with the selected
     *               date and time string upon completion.
     */
    public void showDateTimePicker(EditText target) {
        final Calendar calendar = Calendar.getInstance();


        DatePickerDialog datePickerDialog = new DatePickerDialog(
                context,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    TimePickerDialog timePickerDialog = new TimePickerDialog(
                            context,
                            (timeView, hourOfDay, minute) -> {
                                // This lambda is called when a time is selected.
                                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                calendar.set(Calendar.MINUTE, minute);

                                SimpleDateFormat sdf = new SimpleDateFormat(
                                        "yyyy-MM-dd HH:mm", Locale.getDefault());
                                target.setText(sdf.format(calendar.getTime()));
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            true
                    );
                    timePickerDialog.show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }
}
