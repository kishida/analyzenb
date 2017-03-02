/*
 * Copyright (c) 2017 LINE Corporation. All rights reserved.
 * LINE Corporation PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package kis.analyzenb;

import java.util.AbstractMap;

public class LearningData extends AbstractMap.SimpleEntry<Integer, double[]>{

    public LearningData(Integer key, double[] value) {
        super(key, value);
    }

}
