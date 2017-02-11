/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kis.analyzenb;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 *
 * @author naoki
 */
public class BackPropergation {

    List<Map.Entry<Integer, double[]>> patterns = new ArrayList<>();
    double[][] w;//入力→中間層の係数
    double[] hidden;//中間層→出力の係数
    int dim;//入力パラメータ数
    int hiddendim;//中間層の数+1

    public BackPropergation(int dim, int hiddendim) {
        this.dim = dim;
        this.hiddendim = hiddendim + 1;

        w = new double[this.hiddendim - 1][dim + 1];
        for(int i = 0; i < w.length; ++i){
            for(int j = 0; j < w[i].length; ++j){
                w[i][j] = Math.random() * 2 - 1;
            }
        }
        hidden = new double[this.hiddendim];
        for(int i = 0; i < this.hiddendim; ++i){
            hidden[i] = Math.random() * 2 - 1;
        }
    }



    public void learn(int cls, double[] data) {
        int yi = cls == 1 ? 1 : 0;
        AbstractMap.SimpleEntry<Integer, double[]> entry = new AbstractMap.SimpleEntry<>(yi, data);

        final double k = .3;//学習係数

        double[] pattern = new double[entry.getValue().length + 1];
        for (int i = 0; i < entry.getValue().length; ++i) {
            pattern[i + 1] = entry.getValue()[i];
        }
        pattern[0] = 1;

        int pcls = entry.getKey();//正解
        double[] hiddenvalue = new double[hiddendim];//中間層の出力値
        //入力層→中間層
        for (int j = 0; j < w.length; ++j) {
            double in = 0;
            for (int i = 0; i < pattern.length; ++i) {
                in += pattern[i] * w[j][i];
            }
            hiddenvalue[j + 1] = sigmoid(in);
        }
        hiddenvalue[0] = 1;
        //中間層→出力層
        double out = 0;//出力
        for (int i = 0; i < hiddenvalue.length; ++i) {
            out += hidden[i] * hiddenvalue[i];
        }
        out = sigmoid(out);
        //出力層→中間層
        double p = (pcls - out) * out * (1 - out);
        double[] e = new double[hiddendim];//中間層の補正値
        double[] oldhidden = hidden.clone();//補正前の係数
        for(int i = 0; i < hiddendim; ++i){
            e[i] = p * hiddenvalue[i];
            hidden[i] += e[i] * k;
        }
        //中間層→入力層
        for(int i = 1; i< hiddendim; ++i){
            double ek = e[i] * oldhidden[i] * hiddenvalue[i] * (1 - hiddenvalue[i]);
            for(int j = 0; j < dim + 1; ++j){
                w[i - 1][j] += pattern[j] * ek * k;
            }
        }

    }

    private double sigmoid(double d) {
        return 1 / (1 + Math.exp(-d));
    }

    public int trial(double[] data) {
        double[] pattern = new double[data.length + 1];
        for(int i = 0; i < data.length; ++i){
            pattern[i + 1] = data[i];
        }
        pattern[0] = 1;
            
        double[] hiddendata = new double[hiddendim];
        //入力層→中間層
        for (int j = 0; j < w.length; ++j) {
            double in = 0;
            for (int i = 0; i < pattern.length; ++i) {
                in += pattern[i] * w[j][i];
            }
            hiddendata[j + 1] = sigmoid(in);
        }
        hiddendata[0] = 1;
        //中間層→出力層
        double out = 0;
        for (int i = 0; i < hiddendata.length; ++i) {
            out += hiddendata[i] * hidden[i];
        }
        return (sigmoid(out) > .5) ? 1 : -1;
    }
}
