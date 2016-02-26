package com.saschahuth.brewy.ui.fragment

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.saschahuth.brewy.R
import com.saschahuth.brewy.domain.brewerydb.Api
import com.saschahuth.brewy.domain.brewerydb.DISTANCE_UNIT_MILES
import com.saschahuth.brewy.domain.brewerydb.model.Location
import com.saschahuth.brewy.domain.brewerydb.model.LocationParcel
import com.saschahuth.brewy.domain.brewerydb.model.ResultPage
import com.saschahuth.brewy.ui.activity.LocationDetailsActivity
import com.saschahuth.brewy.ui.adapter.ItemAdapter
import com.saschahuth.brewy.util.hasLocationPermission
import com.saschahuth.brewy.util.requestLocationPermission
import kotlinx.android.synthetic.main.fragment_nearby_breweries.*
import kotlinx.android.synthetic.main.view_drag_header.*
import kotlinx.android.synthetic.main.view_drag_header.view.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import uk.co.chrisjenx.calligraphy.CalligraphyUtils

class NearbyBreweriesFragment : Fragment() {

    private val locationAdapter: ItemAdapter by lazy { ItemAdapter(activity) }

    private val PERMISSIONS_LOCATION = 0

    private var isSortingByName: Boolean = false

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_nearby_breweries, container, false)
    }

    var selectedMarker: Marker? = null

    var markerIcon: BitmapDescriptor? = null
    var selectedMarkerIcon: BitmapDescriptor? = null

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val header = LayoutInflater.from(activity).inflate(R.layout.view_drag_header, null)

        header.sort.setOnClickListener {
            if (isSortingByName) {
                locationAdapter.sortByDistance()
                sort.text = "Sorted by Distance"
            } else {
                locationAdapter.sortByName()
                sort.text = "Sorted by Name"
            }
            isSortingByName = isSortingByName.not()
        }

        val behavior = BottomSheetBehavior.from(recyclerView)
        behavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(view: View, offset: Float) {
            }

            override fun onStateChanged(view: View, state: Int) {
                when (state) {
                    BottomSheetBehavior.STATE_EXPANDED -> selectMarker(null)
                }
            }

        })

        mapView.onCreate(savedInstanceState)

        mapView.getMapAsync {
            it.uiSettings.isMyLocationButtonEnabled = false
            it.setOnMarkerClickListener {
                selectMarker(it)
                true
            }
            it.setOnMapClickListener {
                selectMarker(null)
            }
        }

        markerLocationView.setOnClickListener {
            openDetailsActivity(markerLocationView.boundLocation)
        }

        myLocation.setOnClickListener {
            mapView.getMapAsync {
                it.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(40.024925, -83.0038657), 14F))
            }
        }

        layers.setOnClickListener {
            mapView.getMapAsync {
                it.mapType = if (it.mapType == GoogleMap.MAP_TYPE_NORMAL) GoogleMap.MAP_TYPE_HYBRID else GoogleMap.MAP_TYPE_NORMAL
                layers.text = if (it.mapType == GoogleMap.MAP_TYPE_NORMAL) "Streets" else "Hybrid"
            }
        }

        if (!activity.hasLocationPermission()) {
            activity.requestLocationPermission(PERMISSIONS_LOCATION)
        } else {
            mapView.getMapAsync {
                it.isMyLocationEnabled = true
                it.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(40.024925, -83.0038657), 14F))
            }
        }

        recyclerView.adapter = locationAdapter

        listMapSwitch.setOnCheckedChangeListener {
            button, b ->
            recyclerView.visibility = if (b) View.VISIBLE else View.INVISIBLE
            mapView.visibility = if (b) View.INVISIBLE else View.VISIBLE
            CalligraphyUtils.applyFontToTextView(activity, listLabel, getString(if (b) R.string.fontPathBold else R.string.fontPathRegular))
            CalligraphyUtils.applyFontToTextView(activity, mapLabel, getString(if (b) R.string.fontPathRegular else R.string.fontPathBold))
            myLocation.visibility = if (b) View.GONE else View.VISIBLE
            layers.visibility = if (b) View.GONE else View.VISIBLE
            sort.visibility = if (b) View.VISIBLE else View.GONE
            if (b) {
                selectMarker(null)
            }
        }

        listLabel.setOnTouchListener {
            view, motionEvent ->
            listMapSwitch.dispatchTouchEvent(motionEvent)
        }
        mapLabel.setOnTouchListener {
            view, motionEvent ->
            listMapSwitch.dispatchTouchEvent(motionEvent)
        }

        val breweryDbApi = Api.create()

        breweryDbApi
                .getLocationsByGeoPoint(40.024925, -83.0038657, unit = DISTANCE_UNIT_MILES)
                .enqueue(object : Callback<ResultPage<Location>> {

                    override fun onResponse(call: Call<ResultPage<Location>>?, response: Response<ResultPage<Location>>?) {
                        locationAdapter.addAll(response?.body()?.data?.filterNot {
                            it.inPlanning!! || it.isClosed!!
                        }!!)
                        mapView.getMapAsync {
                            mapView ->
                            response?.body()?.data?.filterNot {
                                it.inPlanning ?: true || it.isClosed ?: true
                            }?.forEach {
                                mapView.addMarker(MarkerOptions()
                                        .position(LatLng(it.latitude?.toDouble() ?: 0.0, it.longitude?.toDouble() ?: 0.0))
                                        .title(it.id)
                                        .icon(markerIcon))
                            }
                        }
                    }

                    override fun onFailure(call: Call<ResultPage<Location>>?, throwable: Throwable?) {
                        //TODO
                    }
                })
    }

    fun selectMarker(marker: Marker?) {
        selectedMarker?.setIcon(markerIcon)
        selectedMarker = marker

        val location = if (marker != null) locationAdapter.findById(marker.title) else null
        if (marker != null && location != null) {
            marker.setIcon(selectedMarkerIcon)
            markerLocationView.bind(location)
            markerLocationView.visibility = View.VISIBLE
        } else {
            markerLocationView.visibility = View.GONE
        }
    }

    fun openDetailsActivity(location: Location?) {
        val locationParcel = LocationParcel.wrap(location)
        val intent = Intent(activity, LocationDetailsActivity::class.java)
        intent.putExtra("location", locationParcel)
        startActivity(intent)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        markerIcon = getBitmapDescriptor(R.drawable.ic_marker_36dp)
        selectedMarkerIcon = getBitmapDescriptor(R.drawable.ic_marker_selected_48dp)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView?.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState!!)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_LOCATION -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mapView.getMapAsync {
                        it.isMyLocationEnabled = true
                    }
                }
            }
        }
    }

    //TODO better caching of result
    fun getBitmapDescriptor(id: Int): BitmapDescriptor {
        val vectorDrawable = ContextCompat.getDrawable(context, id)
        val h = vectorDrawable.intrinsicHeight
        val w = vectorDrawable.intrinsicWidth
        vectorDrawable.setBounds(0, 0, w.toInt(), h.toInt())
        val bm = Bitmap.createBitmap(w.toInt(), h.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bm)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bm)
    }
}