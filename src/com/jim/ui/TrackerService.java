package com.jim.ui;

import com.jim.Params;
import com.jim.tracking.MotionDetector;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

/** Class which runs video tracking in a worker thread.
 *
 */
public class TrackerService extends Service {
    private SimpleObjectProperty<Params> params = new SimpleObjectProperty<Params>(this, "params");
    public final void setParams(Params value) { params.set(value); }
    public final Params getParams() { return params.get(); }
    public final ObjectProperty paramsProperty() { return params; }

    @Override
    protected Task createTask() {
        return new Task() {
            @Override
            protected Object call() {
                Params params = getParams();
                try {
                    new MotionDetector().run(params);
                } catch (Exception e) {
                    System.err.println(e);
                    e.printStackTrace(System.err);
                    throw new RuntimeException(e);
                }
                return null;
            }
        };
    }

}
