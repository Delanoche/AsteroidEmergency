package com.connorhenke.bomb;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

public class Bomb extends Rectangle {

    public Animation animation;
    public float elapsedTime;

    public Bomb(Animation animation) {
        this.elapsedTime = MathUtils.random(0f, 100f);
        this.animation = animation;
    }
}
