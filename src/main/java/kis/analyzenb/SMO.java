/*
 * Copyright (c) 2017 LINE Corporation. All rights reserved.
 * LINE Corporation PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package kis.analyzenb;

import java.util.Arrays;
import java.util.Map;

public class SMO extends LearningMachine{
 double kernel(double[] x1, double[] x2){
        /*
        //多項式カーネル
        double k = 1;
        for(int i = 0; i < x1.length; ++i){
            k += x1[i] * x2[i];
        }
        return k * k;
        */

        //ガウシアンカーネル
        double n = 0;
        for (int i = 0; i < x1.length; i++) {
            n += (x1[i] - x2[i]) * (x1[i] - x2[i]);
        }
        return Math.exp(-n / 3);//5 1.2 .5は分散の2乗

    }

    double[] w;//係数
    double b;//バイアス
    final double c = 100000;//許容範囲？無限大にするとハードマージンになるはずだけど
    final double tol = 0.8;//KKT条件の許容範囲(1 - ε)
    double[] lambda;
    double z = 0;

    @Override
    public void addData(int cls, double[] data) {
        int yi = cls == 1 ? 1 : -1;
        if (Math.random() < 0.05) {
        super.addData(yi, data);
        }
    }

    @Override
    public void learn() {
        System.out.printf("learn %d points%n", patterns.size());
        w = new double[patterns.size()];
        b = 0;

        lambda = new double[patterns.size()];
        for(int i = 0; i < lambda.length; ++i){
            lambda[i] = 0;
        }

        //未定乗数を求める
        boolean alldata = true;//すべてのデータを処理する場合
        boolean changed = false;//変更があった
        eCache = new double[patterns.size()];
        int lp;
        for(lp = 0; lp < 500000 && (alldata || changed); ++lp)  {
            if (lp % 10 == 0) {
                System.out.println(lp);
            }
            changed = false;
            z = 0;
            boolean lastchange = true;
            PROC_LOOP:
            for(int j = 0; j < patterns.size(); ++j){
                //基準点2を選ぶ
                double alpha2 = lambda[j];
                if(!alldata && (alpha2 <= 0 || alpha2 >= c)){// 0 < α < C の点だけ処理する
                    continue;
                }
                if(lastchange){
                    //初回やデータがかわったときキャッシュのクリア
                    Arrays.fill(eCache, 0, eCache.length, Double.NaN);
                    //for(int i = 0; i < eCache.length; ++i) eCache[i] = Double.NaN;
                }
                lastchange = false;

                int t2 = patterns.get(j).getKey();
                double fx2 = calcE(j);

                //KKT条件の判定
                double r2 = fx2 * t2;
                if(!((alpha2 < c && r2 < -tol) || (alpha2 > 0 && r2 > tol))){//KKT条件をみたすなら処理しない
                    continue;
                }
                //基準点1を選ぶ
                //選択法1
                int i = 0;
                int offset = (int)(Math.random() * patterns.size());

                double max = -1;
                for(int ll = 0; ll < patterns.size(); ++ll){//全データにつき
                    int l = (ll + offset) % patterns.size();
                    //0 < α < C
                    if(0 >= lambda[l] || c <= lambda[l]) continue;
                    double dif = Math.abs(calcE(l) - fx2);
                    if(dif > max){
                        max = dif;
                        i = l;
                    }
                }
                if(max >= 0){
                    if(step(i, j)){
                        //処理をしたら次へ
                        changed = true;
                        lastchange = true;
                        continue PROC_LOOP;
                    }
                }
                //選択法2
                offset = (int)(Math.random() * patterns.size());//ランダムな位置から
                for(int l = 0; l < patterns.size(); ++l){
                    //0 < α < C
                    i = (l + offset) % patterns.size();
                    if(0 >= lambda[i] || c <= lambda[i]) continue;
                    if(step(i, j)){
                        //処理をしたら次へ
                        changed = true;
                        lastchange = true;
                        continue PROC_LOOP;
                    }
                }
                //選択法3
                offset = (int)(Math.random() * patterns.size());//ランダムな位置から
                for(int l = 0; l < patterns.size(); ++l){
                    i = (l + offset) % patterns.size();
                    if(step(i, j)){
                        //処理をしたら次へ
                        changed = true;
                        lastchange = true;
                        continue PROC_LOOP;
                    }
                }
            }

            //すべてのデータを処理しても処理するものがなければ終了になる
            if(z < 0.01) changed = false;
            if(alldata){
                alldata = false;
            }else{
                if(changed) alldata = true;
            }
        }
        System.out.println("収束回数" + lp);

        //wの値を求める
        for(int i = 0; i < w.length; ++i){
            w[i] = lambda[i] * patterns.get(i).getKey();
        }
        //bを求める
        b = 0;
        int count = 0;
        for(int i = 0; i < lambda.length; ++i){
            if(Math.abs(w[i]) <= 0.05) continue;
            for(int l = 0; l < w.length; ++l){
                b -= w[l] * kernel(
                        patterns.get(i).getValue(), patterns.get(l).getValue());
            }
            ++count;
        }
        b /= count;

        System.out.printf("support vectors %d%n", lambda.length);
        /*
        for(int i = 0; i < lambda.length; ++i){
            System.out.printf("%.4f ", lambda[i]);
        }
        System.out.println();*/
    }

 @Override
    public int trial(double[] data) {
        double s = b;
        for(int i = 0; i < w.length; ++i){
            Map.Entry<Integer, double[]> p = patterns.get(i);
            s += w[i] * kernel(data, p.getValue());
        }
        return s > 0 ? 1 : -1;
    }

    private double[] eCache;
    private double calcE(int i){
        if(!Double.isNaN(eCache[i])) return eCache[i];
        double e = b - patterns.get(i).getKey();
        for(int j = 0; j < lambda.length; ++j){
            e += lambda[j] * patterns.get(j).getKey() *
                    kernel(patterns.get(j).getValue(), patterns.get(i).getValue());
        }
        eCache[i] = e;
        return e;
    }

    /** 実際の計算処理 */
    private boolean step(int i, int j) {
        if(i == j) return false;
        double fx2 = calcE(j);

        int t1 = patterns.get(i).getKey();
        int t2 = patterns.get(j).getKey();

        double fx1 = calcE(i);

        //基準点2を計算
        double k11 = kernel(patterns.get(i).getValue(), patterns.get(i).getValue());
        double k22 = kernel(patterns.get(j).getValue(), patterns.get(j).getValue());
        double k12 = kernel(patterns.get(i).getValue(), patterns.get(j).getValue());
        double eta = k11 + k22 - 2 * k12;
        if(eta <= 0) return false;
        double newwj = lambda[j] + t2 * (fx1 - fx2) / eta;
        //クリッピング
        double u;
        double v;
        if(t1 == t2){
            u = Math.max(0, lambda[j] + lambda[i] - c);
            v = Math.min(c, lambda[j] + lambda[i]);
        }else{
            u = Math.max(0, lambda[j] - lambda[i]);
            v = Math.min(c, c + lambda[j] - lambda[i]);
        }
        if(u == v) return false;
        newwj = Math.max(u, newwj);
        newwj = Math.min(v, newwj);

        //基準点2から基準点1を計算
        z += Math.abs(lambda[j] - newwj);
        lambda[i] += t1 * t2 * (lambda[j] - newwj);
        lambda[j] = newwj;
        return true;
    }


}
