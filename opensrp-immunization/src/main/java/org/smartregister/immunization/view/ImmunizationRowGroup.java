package org.smartregister.immunization.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.domain.Alert;
import org.smartregister.immunization.R;
import org.smartregister.immunization.adapter.ImmunizationRowAdapter;
import org.smartregister.immunization.domain.Vaccine;
import org.smartregister.immunization.domain.VaccineWrapper;
import org.smartregister.immunization.domain.jsonmapping.VaccineGroup;
import org.smartregister.util.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Created by raihan on 13/03/2017.
 */
public class ImmunizationRowGroup extends LinearLayout implements View.OnClickListener,
        ImmunizationRowCard.OnVaccineStateChangeListener {
    private Context context;
    private TextView nameTV;
    private TextView recordAllTV;
    private ExpandableHeightGridView vaccinesGV;
    private ImmunizationRowAdapter vaccineCardAdapter;
    private VaccineGroup vaccineData;
    private CommonPersonObjectClient childDetails;
    private List<Vaccine> vaccineList;
    private List<Alert> alertList;
    private State state;
    public boolean editmode;
    private OnRecordAllClickListener onRecordAllClickListener;
    private OnVaccineClickedListener onVaccineClickedListener;
    private OnVaccineUndoClickListener onVaccineUndoClickListener;
    private SimpleDateFormat READABLE_DATE_FORMAT = new SimpleDateFormat("dd MMMM, yyyy", Locale.US);
    private boolean modalOpen;

    private static enum State {
        IN_PAST,
        CURRENT,
        IN_FUTURE
    }

    public ImmunizationRowGroup(Context context, boolean editmode) {
        super(context);
        this.editmode = editmode;
        init(context);
    }

    public ImmunizationRowGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ImmunizationRowGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public CommonPersonObjectClient getChildDetails() {
        return this.childDetails;
    }

    public VaccineGroup getVaccineData() {
        return this.vaccineData;
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ImmunizationRowGroup(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        this.context = context;
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.view_immunization_row_group, this, true);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        setLayoutParams(layoutParams);
        nameTV = (TextView) findViewById(R.id.name_tv);
        nameTV.setVisibility(GONE);
        vaccinesGV = (ExpandableHeightGridView) findViewById(R.id.vaccines_gv);
        vaccinesGV.setExpanded(true);
        recordAllTV = (TextView) findViewById(R.id.record_all_tv);
        recordAllTV.setOnClickListener(this);
        recordAllTV.setVisibility(GONE);
    }

    public void setData(VaccineGroup vaccineData, CommonPersonObjectClient childDetails, List<Vaccine> vaccines, List<Alert> alerts) {
        this.vaccineData = vaccineData;
        this.childDetails = childDetails;
        this.vaccineList = vaccines;
        this.alertList = alerts;
        updateViews();
    }


    public void setOnVaccineUndoClickListener(OnVaccineUndoClickListener onVaccineUndoClickListener) {
        this.onVaccineUndoClickListener = onVaccineUndoClickListener;
    }

    /**
     * This method will update all views, including vaccine cards in this group
     */
    public void updateViews() {
        updateViews(null);
    }

    /**
     * This method will update vaccine group views, and the vaccine cards corresponding to the list
     * of {@link VaccineWrapper}s specified
     *
     * @param vaccinesToUpdate List of vaccines who's views we want updated, or NULL if we want to
     *                         update all vaccine views
     */
    public void updateViews(ArrayList<VaccineWrapper> vaccinesToUpdate) {
        this.state = State.IN_PAST;
        if (this.vaccineData != null) {
            String dobString = Utils.getValue(childDetails.getColumnmaps(), "dob", false);
            DateTime dateTime = !dobString.isEmpty() ? new DateTime(dobString) : new DateTime();
            Date dob = dateTime.toDate();
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            long timeDiff = today.getTimeInMillis() - dob.getTime();

            if (timeDiff < today.getTimeInMillis()) {
                this.state = State.IN_PAST;
            } else if (timeDiff > (today.getTimeInMillis() + TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS))) {
                this.state = State.IN_FUTURE;
            } else {
                this.state = State.CURRENT;
            }
            updateStatusViews();
            updateVaccineCards(vaccinesToUpdate);

        }
    }

    private void updateStatusViews() {
        switch (this.state) {
            case IN_PAST:
                nameTV.setText(vaccineData.name);
                break;
            case CURRENT:
                nameTV.setText(String.format(context.getString(R.string.due_),
                        vaccineData.name, context.getString(R.string.today)));
                break;
            case IN_FUTURE:
                String dobString = Utils.getValue(childDetails.getColumnmaps(), "dob", false);
                Calendar dobCalender = Calendar.getInstance();
                DateTime dateTime = new DateTime(dobString);
                dobCalender.setTime(dateTime.toDate());
                dobCalender.add(Calendar.DATE, vaccineData.days_after_birth_due);
                nameTV.setText(String.format(context.getString(R.string.due_),
                        vaccineData.name,
                        READABLE_DATE_FORMAT.format(dobCalender.getTime())));
                break;
            default:
                break;
        }
    }

    private void updateVaccineCards(ArrayList<VaccineWrapper> vaccinesToUpdate) {
        if (vaccineCardAdapter == null) {
            vaccineCardAdapter = new ImmunizationRowAdapter(context, this, editmode, vaccineList, alertList);
            vaccinesGV.setAdapter(vaccineCardAdapter);
        }

        if (vaccineCardAdapter != null) {
            vaccineCardAdapter.update(vaccinesToUpdate);
            toggleRecordAllTV();
        }
    }

    public boolean isModalOpen() {
        return modalOpen;
    }

    public void setModalOpen(boolean modalOpen) {
        this.modalOpen = modalOpen;
    }


    public void toggleRecordAllTV() {
        if (vaccineCardAdapter.getDueVaccines().size() > 0) {
            recordAllTV.setVisibility(GONE);
        } else {
            recordAllTV.setVisibility(GONE);
        }
    }

    @Override
    public void onClick(View v) {
        v.setEnabled(false);
        if (v.equals(recordAllTV)) {
            if (onRecordAllClickListener != null && vaccineCardAdapter != null) {
                onRecordAllClickListener.onClick(this, vaccineCardAdapter.getDueVaccines());
            }
        } else if (v instanceof ImmunizationRowCard && onVaccineClickedListener != null) {
            onVaccineClickedListener.onClick(this, ((ImmunizationRowCard) v).getVaccineWrapper());

        } else if (v.getId() == R.id.undo_b && v.getParent().getParent() instanceof ImmunizationRowCard) {
            ImmunizationRowCard vaccineCard = (ImmunizationRowCard) v.getParent().getParent();
            onUndoClick(vaccineCard);
        }
        v.setEnabled(true);
    }

    @Override
    public void onStateChanged(ImmunizationRowCard.State newState) {
        updateViews();
    }

    public void onUndoClick(ImmunizationRowCard vaccineCard) {
        if (this.onVaccineUndoClickListener != null) {
            this.onVaccineUndoClickListener.onUndoClick(this, vaccineCard.getVaccineWrapper());
        }
    }

    public void setOnRecordAllClickListener(OnRecordAllClickListener onRecordAllClickListener) {
        this.onRecordAllClickListener = onRecordAllClickListener;
    }

    public void setOnVaccineClickedListener(OnVaccineClickedListener onVaccineClickedListener) {
        this.onVaccineClickedListener = onVaccineClickedListener;
    }

    public static interface OnRecordAllClickListener {
        void onClick(ImmunizationRowGroup vaccineGroup, ArrayList<VaccineWrapper> dueVaccines);
    }

    public static interface OnVaccineClickedListener {
        void onClick(ImmunizationRowGroup vaccineGroup, VaccineWrapper vaccine);
    }

    public static interface OnVaccineUndoClickListener {
        void onUndoClick(ImmunizationRowGroup vaccineGroup, VaccineWrapper vaccine);
    }

    public ArrayList<VaccineWrapper> getDueVaccines() {
        if (vaccineCardAdapter != null) {
            return vaccineCardAdapter.getDueVaccines();
        }
        return new ArrayList<VaccineWrapper>();
    }

    public List<Vaccine> getVaccineList() {
        return vaccineList;
    }

    public void setVaccineList(List<Vaccine> vaccineList) {
        this.vaccineList = vaccineList;
    }

    public List<Alert> getAlertList() {
        return alertList;
    }

    public void setAlertList(List<Alert> alertList) {
        this.alertList = alertList;
    }

    public void updateWrapperStatus(ArrayList<VaccineWrapper> wrappers) {
        if (vaccineCardAdapter != null) {
            vaccineCardAdapter.updateWrapperStatus(wrappers, getChildDetails());
        }
    }

    public void updateWrapper(VaccineWrapper wrapper) {
        if (vaccineCardAdapter != null) {
            vaccineCardAdapter.updateWrapper(wrapper);
        }
    }

}
