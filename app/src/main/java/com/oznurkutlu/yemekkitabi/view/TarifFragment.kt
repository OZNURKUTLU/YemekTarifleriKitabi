package com.oznurkutlu.yemekkitabi.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.ImageDecoder.createSource
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import androidx.room.Room
import com.google.android.material.snackbar.Snackbar
import com.oznurkutlu.yemekkitabi.databinding.FragmentTarifBinding
import com.oznurkutlu.yemekkitabi.model.Tarif
import com.oznurkutlu.yemekkitabi.roomdb.TarifDAO
import com.oznurkutlu.yemekkitabi.roomdb.TarifDatabase
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.ByteArrayOutputStream
import java.io.IOException


private  lateinit var db : TarifDatabase
private  lateinit var tarifDao: TarifDAO
private lateinit var permissionLauncher: ActivityResultLauncher<String>
private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
private var secilenGorsel : Uri?= null
private var secilenBitmap : Bitmap?= null
private val mDisposable = CompositeDisposable()
private var secilenTarif : Tarif?=null

class TarifFragment : Fragment() {
    private var _binding: FragmentTarifBinding? = null
    private val binding get() = _binding!!




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerLauncher()

        db=Room.databaseBuilder(requireContext(), TarifDatabase::class.java,"Tarifler").build()
        tarifDao= db.tarifDao()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTarifBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.imageView.setOnClickListener{gorselSec(it)}
        binding.kaydetButton.setOnClickListener{kaydet(it)}
        binding.silButton.setOnClickListener{sil(it)}
        arguments?.let {
            val bilgi= TarifFragmentArgs.fromBundle(it).bilgi
            if (bilgi=="yeni"){
                //Yeni tarif eklenecek
                secilenTarif = null
                binding.silButton.isEnabled = false
                binding.kaydetButton.isEnabled = true
                binding.isimText.setText("")
                binding.malzemeText.setText("")
            } else {
                //eski eklenmiş tarif gösteriliyor
                binding.silButton.isEnabled = true
                binding.kaydetButton.isEnabled = false
                val id = TarifFragmentArgs.fromBundle(it).id

                mDisposable.add(
                    tarifDao.findById(id)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers
                        .mainThread()).subscribe(this::handleResponse)
                )


            }
        }
    }

    private fun handleResponse(tarif: Tarif){
        val bitmap = BitmapFactory.decodeByteArray(tarif.gorsel,0,tarif.gorsel.size)
        binding.imageView.setImageBitmap(bitmap)
        binding.isimText.setText(tarif.isim)
        binding.malzemeText.setText(tarif.malzeme)
        secilenTarif = tarif

    }

  /**  fun kaydet(view: View) {
        val isim = binding.isimText.text.toString()
        val malzeme = binding.malzemeText.text.toString()
        if (secilenBitmap != null) {
            val kucukBitmap = kucukBitmapOlustur(secilenBitmap!!, 300)
            val outputStream =ByteArrayOutputStream()
            kucukBitmap.compress(Bitmap.CompressFormat.PNG,50,outputStream)
            val byteDizisi = outputStream.toByteArray()
            val tarif = Tarif(isim = isim, malzeme = malzeme, gorsel = byteDizisi)

            //RxJava

            mDisposable.add(tarifDao.insert(tarif).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleResponseForInsert))
        }
    } **/

 fun kaydet(view: View) {
        val isim = binding.isimText.text.toString()
        val malzeme = binding.malzemeText.text.toString()

        if (isim.isNotEmpty() && malzeme.isNotEmpty()) {
            val kucukBitmap = secilenBitmap?.let { kucukBitmapOlustur(it, 300) }
            val outputStream = ByteArrayOutputStream()
            kucukBitmap?.compress(Bitmap.CompressFormat.PNG, 50, outputStream)
            val byteDizisi = outputStream.toByteArray()

            if (secilenTarif != null) {
                // Güncelleme işlemi
                val guncellenenTarif = Tarif(
                    id = secilenTarif!!.id,
                    isim = isim,
                    malzeme = malzeme,
                    gorsel = byteDizisi
                )

                mDisposable.add(tarifDao.update(guncellenenTarif)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleResponseForInsert))
            } else {
                // Yeni tarif ekleme
                val yeniTarif = Tarif(
                    isim = isim,
                    malzeme = malzeme,
                    gorsel = byteDizisi
                )

                mDisposable.add(tarifDao.insert(yeniTarif)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleResponseForInsert))
            }
        } else {
            Toast.makeText(requireContext(), "Lütfen tüm alanları doldurun.", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleResponseForInsert(){
        //bir önceki fragment a don
        val action = TarifFragmentDirections.actionTarifFragmentToListeFragment()
        Navigation.findNavController(requireView()).navigate(action)
    }
    fun sil(view: View){
        if (secilenTarif != null) {
            mDisposable.add(tarifDao.delete(tarif = secilenTarif!!)
                .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleResponseForInsert))
        }
    }
    fun gorselSec(view: View){
        activity?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        requireContext().applicationContext,
                        Manifest.permission.READ_MEDIA_IMAGES
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    //izin verilmemis, izin istememiz gerekiyor.
                    if (ActivityCompat.shouldShowRequestPermissionRationale(
                            requireActivity(),
                            Manifest.permission.READ_MEDIA_IMAGES
                        )
                    ) {
                        //snackbar göstermemiz lazım, kullanıcıdan neden izin istediğimizi tekrar söylerek izin almamız lazım
                        Snackbar.make(
                            view,
                            "Galeriye ulaşıp görsel seçmemiz lazım",
                            Snackbar.LENGTH_INDEFINITE
                        ).setAction(
                            "İzin Ver", View.OnClickListener {

                                //izin isteyeceğiz
                                permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                            }
                        ).show()
                    } else {
                        //izin isteyeceğiz
                        permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                    }
                } else {
                    //izin verilmiş, galeriye gidebilirim
                    val intentToGallery =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    activityResultLauncher.launch(intentToGallery)
                }


            } else {
                if (ContextCompat.checkSelfPermission(
                        requireContext().applicationContext,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    //izin verilmemis, izin istememiz gerekiyor.
                    if (ActivityCompat.shouldShowRequestPermissionRationale(
                            requireActivity(),
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        )
                    ) {
                        //snackbar göstermemiz lazım, kullanıcıdan neden izin istediğimizi tekrar söylerek izin almamız lazım
                        Snackbar.make(
                            view,
                            "Galeriye ulaşıp görsel seçmemiz lazım",
                            Snackbar.LENGTH_INDEFINITE
                        ).setAction(
                            "İzin Ver", View.OnClickListener {

                                //izin isteyeceğiz
                                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                        ).show()
                    } else {
                        //izin isteyeceğiz
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                } else {
                    //izin verilmiş, galeriye gidebilirim
                    val intentToGallery =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    activityResultLauncher.launch(intentToGallery)
                }


            }

        }


    }

    private  fun registerLauncher(){
        activityResultLauncher=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result->
            if(result.resultCode==AppCompatActivity.RESULT_OK){
                val intentFromResult=result.data
                if(intentFromResult!= null){
                    secilenGorsel=intentFromResult.data
                    try {
                        if (Build.VERSION.SDK_INT >= 28) {
                            val source = createSource(
                                requireActivity().contentResolver,
                                secilenGorsel!!
                            )
                            secilenBitmap = ImageDecoder.decodeBitmap(source)
                            binding.imageView.setImageBitmap(secilenBitmap)
                        } else {
                            secilenBitmap = MediaStore.Images.Media.getBitmap(
                                requireActivity().contentResolver,
                                secilenGorsel
                            )
                            binding.imageView.setImageBitmap(secilenBitmap)
                        }
                    } catch(e: IOException){
                        println(e.localizedMessage)
                    }
                    }
            }
        }

        permissionLauncher= registerForActivityResult(ActivityResultContracts.RequestPermission()){
            result->
            if(result){
                //izin verildi
                //Galeriye gidebiliriz
                val intentToGallery=Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }else{
                //izin verilmedi
                Toast.makeText(requireContext(),"İzin verilmedi!",Toast.LENGTH_LONG).show()
            }
        }

    }

    private fun kucukBitmapOlustur(kullacininSectigiBitmap : Bitmap, maximumBoyut : Int):Bitmap{
        var width = kullacininSectigiBitmap.width
        var height = kullacininSectigiBitmap.height

        val bitmapOrani : Double =width.toDouble() / height.toDouble()
        if (bitmapOrani>1){
            //gorsel yatay
            width=maximumBoyut
            val kisaltilmisYukseklik = width / bitmapOrani
            height = kisaltilmisYukseklik.toInt()
        }else{
            //gorsel dikey
            height=maximumBoyut
            val kisaltilmisGenislik = height * bitmapOrani
            width = kisaltilmisGenislik.toInt()
        }


        return Bitmap.createScaledBitmap(kullacininSectigiBitmap,100,100,true)
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        mDisposable.clear()
    }

}
