package com.jim.ui;

import com.jim.Params;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;

import java.util.function.Consumer;

public class ParamsForm {
    @FXML
    Slider minContourSlider;
    @FXML
    Label minContourLabel;
    public Label maxContourLabel;
    public Slider maxContourSlider;
    @FXML public Label minContourLenLabel;
    @FXML public Slider minContourLenSlider;
    @FXML public Label maxContourLenLabel;
    @FXML public Slider maxContourLenSlider;
    private Params params;

    public void initialize() {
        tieSliderToLabel(minContourSlider, minContourLabel, value -> params.trParams.minContourArea = value);
        tieSliderToLabel(maxContourSlider, maxContourLabel, value -> params.trParams.maxContourArea = value);
        tieSliderToLabel(minContourLenSlider, minContourLenLabel, value -> params.trParams.minContourLength = value);
        tieSliderToLabel(maxContourLenSlider, maxContourLenLabel, value -> params.trParams.maxContourLength = value);
    }

    public void setParams(Params params) {

        this.params = params;
        minContourLabel.setText(String.valueOf(params.trParams.minContourArea));
        minContourSlider.setValue(params.trParams.minContourArea);
        maxContourLabel.setText(String.valueOf(params.trParams.maxContourArea));
        maxContourSlider.setMax(Math.max(2 * params.trParams.maxContourArea, maxContourSlider.getMax()));
        maxContourSlider.setValue(params.trParams.maxContourArea);
        minContourLenLabel.setText(String.valueOf(params.trParams.minContourLength));
        minContourLenSlider.setValue(params.trParams.minContourLength);
        maxContourLenLabel.setText(String.valueOf(params.trParams.maxContourLength));
        double max = Math.min(100000, Math.max(2 * params.trParams.maxContourLength, maxContourSlider.getMax()));
        maxContourLenSlider.setMax(max);
        maxContourLenSlider.setValue(params.trParams.maxContourLength);
    }

    // =================================================================
    // Private methods

    private void tieSliderToLabel(Slider slider, Label label, Consumer<Double> paramSetter) {
        slider.valueProperty().addListener((observable, oldValue, newValue) -> {
            double value = newValue.doubleValue();
            label.setText(Double.toString(value));
            paramSetter.accept(value);
        });

    }
}
