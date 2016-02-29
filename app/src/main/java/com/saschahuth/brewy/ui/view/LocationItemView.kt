package com.saschahuth.brewy.ui.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ActivityOptionsCompat
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import com.bumptech.glide.Glide
import com.saschahuth.brewy.R
import com.saschahuth.brewy.domain.brewerydb.model.Location
import com.saschahuth.brewy.domain.brewerydb.model.LocationParcel
import com.saschahuth.brewy.ui.activity.LocationDetailsActivity
import com.saschahuth.brewy.util.*
import kotlinx.android.synthetic.main.item_brewery.view.*

/**
 * Created by sascha on 24.02.16.
 */
class LocationItemView : RelativeLayout {

    var boundLocation: Location? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        setBackgroundResource(R.color.defaultBackground)
        LayoutInflater.from(context).inflate(R.layout.item_brewery, this, true)

        setOnClickListener {
            openDetailsActivity(boundLocation)
        }
    }

    fun bind(location: Location) {
        boundLocation = location
        title.text = location.getFormattedName()
        address.text = location.getFormattedAddress(includeCountry = false)

        distance.text = "${location.distanceTo(40.024925, -83.0038657).toInt()} m away" //TODO just for testing
        val uriString = location.brewery?.images?.squareMedium
        if (uriString != null) {
            Glide.with(context).load(uriString).bitmapTransform(RoundedCornersTransformation(context, dimenToPixels(R.dimen.marginSmall), 0)).into(image)
        } else {
            Glide.clear(image);
            image.setImageResource(R.color.imagePlaceholder)
        }
    }

    fun openDetailsActivity(location: Location?) {
        val locationParcel = LocationParcel.wrap(location)
        val intent = Intent(context, LocationDetailsActivity::class.java)
        intent.putExtra("location", locationParcel)

        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                // the context of the activity
                context as Activity,
                android.support.v4.util.Pair<View, String>(image,
                        context.getString(R.string.transitionNameCircle))
        )

        ActivityCompat.startActivity(context as Activity, intent, options.toBundle())
    }
}