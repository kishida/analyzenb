/*
 * Copyright (c) 2017 LINE Corporation. All rights reserved.
 * LINE Corporation PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package kis.analyzenb;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class LearningMachine {
    List<Map.Entry<Integer, double[]>> patterns = new ArrayList<>();
    public void addData(int cls, double[] data) {
        patterns.add(new AbstractMap.SimpleEntry<>(cls, data));
    }

    public abstract void learn();
    public abstract int trial(double[] data);
}
