package pt.isec.tp_amov.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import pt.isec.tp_amov.R
import pt.isec.tp_amov.model.Model
import pt.isec.tp_amov.model.ModelView
import pt.isec.tp_amov.objects.Product
import java.io.ByteArrayOutputStream


/**
 * This activity is going to be responsible for the creation and edition of a product
 */

class ManageProductActivity : AppCompatActivity(){
    private var dialogNewCategory: AlertDialog? = null
    private var dialogNewUnit: AlertDialog? = null

    private lateinit var spCategory: Spinner
    private lateinit var spUnit: Spinner
    private lateinit var type: String
    private lateinit var imageView: ImageView

    private var dataName: String? = null
    private var dataCat: String? = null
    private var listId = -1
    private var prodId = -1

    private var bitmap: Bitmap? = null
    var editText: EditText? = null

    /**
     * Camera vals
     */
    private val cameraPermissionCode = 101
    private val galleryPermissionCode = 102
    private val cameraIntentCode = 11
    private val galleryIntentCode = 12

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_product)
        imageView = findViewById(R.id.productImageView)
        getIntents()
        prepareSpinners()
        checkReceivedType()
        handleCurrency()
        handleModelView(savedInstanceState)
    }
    override fun onSaveInstanceState(outState: Bundle) {
        if (ModelView.dialogNewCategoryShowing) {
            if (editText != null)
                ModelView.dialogTextProd = editText!!.text.toString()
        }
        if (ModelView.dialogNewUnitsShowing) {
            if (editText != null)
                ModelView.dialogTextProd = editText!!.text.toString()
        }
        try {
            if  (bitmap != null)
                Model.bitmap = bitmap
        } catch (e: KotlinNullPointerException) {}
        super.onSaveInstanceState(outState)
    }
    override fun onDestroy() {
        Model.save(applicationContext)
        if (dialogNewUnit != null)
            if (dialogNewUnit!!.isShowing)
                dialogNewUnit!!.dismiss()
        if (dialogNewCategory != null)
            if (dialogNewCategory!!.isShowing)
                dialogNewCategory!!.dismiss()

        super.onDestroy()
    }

    override fun onStop() {
        Model.save(applicationContext)
        super.onStop()
    }

    //onCreate functions
    private fun getIntents() { //get intents passed through the extras
        listId = intent.getIntExtra("listId", -1)
        prodId = intent.getIntExtra("productId", -1)
        type = intent.getStringExtra("type")!!
        dataName = intent.getStringExtra("dataName")
        dataCat = intent.getStringExtra("dataCat")

        //Verify if the ID is valid
        if(listId == -1){
            finish()
        }

    }
    private fun prepareSpinners() { //prepare spinners with categories
        //Handle spinners
        spCategory = findViewById(R.id.spinnerCat)
        spUnit = findViewById(R.id.spinnerUnit)
        loadCategories()
        loadUnits()
    }
    private fun checkReceivedType() { //check if product is being reused (from searchProductActivity) or edit another product
        //Check which type was received
        if(prodId != -1 && type == "edit"){
            fillOptions()
        }
        else if(type == "reuseData" && dataName != null && dataCat != null){
            fillPartialOpts()
        }
    }
    private fun handleCurrency() { //changes currency depending on the language
        val currency = findViewById<TextView>(R.id.currency)
        currency.text = getString(R.string.currency)
    }
    private fun handleModelView(savedInstanceState: Bundle?) { //Restore state after the orientation change
        if (savedInstanceState != null) {
            if (ModelView.dialogNewCategoryShowing) {
                onNewCategory(findViewById(R.id.addNewCategory))
                editText!!.setText(ModelView.dialogTextProd)
            }
            if (ModelView.dialogNewUnitsShowing) {
                onNewUnitType(findViewById(R.id.addNewUnit))
                editText!!.setText(ModelView.dialogTextProd)
            }
            try {
                if (Model.getProdById(prodId, listId)!!.image != null) { //checks if bitmap byteArray is null
                    bitmap = transformIntoBitmap(Model.getProdById(prodId, listId)!!.image)
                    imageView.setImageBitmap(bitmap)
                    return //in case there is already a bitmap on display, return
                }
            } catch (e: KotlinNullPointerException) { }
            try {
                if (Model.bitmap != null) { //checks if bitmap is null. For when a new product has not been saved yet
                    bitmap = Model.bitmap
                    imageView.setImageBitmap(bitmap)
                }
            } catch (e: KotlinNullPointerException) { }
        }
    }

    //Create the option on the menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_new_product, menu)
        if(type == "create" || type == "reuseData"){
            supportActionBar?.title = getString(R.string.titleAddProdList) + " " + Model.getListById(listId)?.name
            menu!!.getItem(0).isVisible = true
            menu.getItem(1).isVisible = false
        }
        else{
            supportActionBar?.title = getString(R.string.titleEditProdList) + " " + Model.getListById(listId)?.name
            menu!!.getItem(0).isVisible = false
            menu.getItem(1).isVisible = true
        }
        return true
    }

    //Will handle the items clicked by the user on the menu
    override fun onOptionsItemSelected(item: MenuItem): Boolean { //Process on menu item selected (there is only one)
        val name: String = findViewById<EditText>(R.id.edProductName).text.toString()
        val brand: String = findViewById<EditText>(R.id.edBrand).text.toString()
        val notes: String = findViewById<EditText>(R.id.edNotes).text.toString()
        val quantity: String = findViewById<EditText>(R.id.edQuantity).text.toString()
        val price: String = findViewById<EditText>(R.id.edPrice).text.toString()

        if(!testFields(name, quantity)){
            return false
        }
        testBitmap()

        if(item.itemId == R.id.newProdCheck) {
            optNewProd(name, brand, price, quantity, notes)
        }

        if(item.itemId == R.id.editProdCheck){
            val prod = Model.getProdById(prodId, listId)
            if(prod!!.name != name) {
                addNew(prod, name, brand, price, quantity, notes)
            }
            else {
                //If the product is in the database and it was modified, we're just going to modify our product
                if (price != "") {
                    if (prod.price == price.toDouble()) {
                        samePrice(prod, name, brand, price, quantity, notes)
                    } else {
                        newPrice(prod, name, brand, price, quantity, notes)
                    }
                } else{
                    newPrice(prod, name, brand, price, quantity, notes)
                }
            }
            finish()
        }
        return super.onOptionsItemSelected(item)
    }
    private fun testFields(name: String, quantity: String): Boolean { //Tests mandatory fields
        if (name.isEmpty()) {
            Toast.makeText(applicationContext, getString(R.string.no_product_name), Toast.LENGTH_LONG).show()
            return false
        }
        if (quantity == "0") {
            Toast.makeText(applicationContext, getString(R.string.no_product_quantity), Toast.LENGTH_LONG).show()
            return false
        }
        if (getUnit() == "") {
            Toast.makeText(applicationContext, getString(R.string.must_create_unit), Toast.LENGTH_LONG).show()
            return false
        }
        if (getCategory() == "") {
            Toast.makeText(applicationContext, getString(R.string.must_create_category), Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }
    private fun testBitmap() { //test if there is a bitmap associated with the imageView
        this.bitmap = try {
            val bitmapDrawable: BitmapDrawable = imageView.drawable as BitmapDrawable
            bitmapDrawable.bitmap
        } catch (e: TypeCastException) {
            null
        }
    }
    private fun optNewProd(name: String, brand: String, price: String, quantity: String, notes: String) {
        if(price == "") {
            Model.receiveProduct(
                    name,
                    brand,
                    0.0,
                    quantity.toDouble(),
                    getUnit(),
                    getCategory(),
                    notes,
                    transformIntoByteArray(bitmap),
                    listId
            )

        }else {
            Model.receiveProduct(
                    name,
                    brand,
                    price.toDouble(),
                    quantity.toDouble(),
                    getUnit(),
                    getCategory(),
                    notes,
                    transformIntoByteArray(bitmap),
                    listId
            )
        }
        finish()
    }
    private fun addNew(prod: Product, name: String, brand: String, price: String, quantity: String, notes: String) {
        //If the name of the product changed and the product doesn't exist in the database, adds the product to the "database" and to the list
        //We cant forget to update the database, because we got one item that is not being used anymore
        Model.updateData(
                prod.name,
                prod.category,
                prod.price,
                name,
                getCategory(),
                price.toDouble()
        )
        prod.editProduct(
                name,
                brand,
                price.toDouble(),
                quantity.toDouble(),
                getUnit(),
                getCategory(),
                transformIntoByteArray(bitmap),
                notes
        )
    }
    private fun samePrice(prod: Product, name: String, brand: String, price: String, quantity: String, notes: String) {
        if(price == ""){
            prod.editProduct(
                    name,
                    brand,
                    0.0,
                    quantity.toDouble(),
                    getUnit(),
                    getCategory(),
                    transformIntoByteArray(bitmap),
                    notes
            )
        }else {
            prod.editProduct(
                    name,
                    brand,
                    price.toDouble(),
                    quantity.toDouble(),
                    getUnit(),
                    getCategory(),
                    transformIntoByteArray(bitmap),
                    notes
            )
        }
    }
    private fun newPrice(prod: Product, name: String, brand: String, price: String, quantity: String, notes: String) {
        if(price == ""){
            Model.updateDataPrices(
                    prod.name,
                    prod.category,
                    prod.price,
                    name,
                    getCategory(),
                    0.0
            )
            prod.editProduct(
                    name,
                    brand,
                    0.0,
                    quantity.toDouble(),
                    getUnit(),
                    getCategory(),
                    transformIntoByteArray(bitmap),
                    notes
            )
        }else {
            Model.updateDataPrices(
                    prod.name,
                    prod.category,
                    prod.price,
                    name,
                    getCategory(),
                    price.toDouble()
            )
            prod.editProduct(
                    name,
                    brand,
                    price.toDouble(),
                    quantity.toDouble(),
                    getUnit(),
                    getCategory(),
                    transformIntoByteArray(bitmap),
                    notes
            )
        }
    }

    //onClicks of the buttons '+' and '-' image buttons
    fun onIncQuantity(view: View) {
        val editText: EditText = findViewById(R.id.edQuantity)
        val text: String = editText.text.toString()
        try { //In case it is an Integer
            var num: Int = text.toInt()
            num += 1
            editText.setText(num.toString())

            Log.i("onQuantityInc int", num.toString())
        }
        catch (nfe: NumberFormatException) { //In case it is a double
            var num: Double = text.toDouble()
            num += 1.0
            editText.setText(num.toString())

            Log.i("onQuantityInc double", num.toString())
        }
    }
    fun onDecQuantity(view: View) {
        val editText: EditText = findViewById(R.id.edQuantity)
        val text: String = editText.text.toString()
        try { //In case it is an Integer
            var num: Int = text.toInt()
            if (num - 1 <= 0)
                num = 0
            else
                num -= 1
            editText.setText(num.toString())

            Log.i("onQuantityDec int: ", num.toString())
        }
        catch (nfe: NumberFormatException) { //In case it is a double
            var num: Double = text.toDouble()
            if (num - 1.0 <= 0.0)
                num = 0.0
            else
                num -= 1.0
            editText.setText(num.toString())

            Log.i("onQuantityDec double: ", num.toString())
        }
    }

    //onClicks of the buttons on this layout
    fun onOpenCamera(view: View) {
        //Ask for camera permissions
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), cameraPermissionCode)
        else { //In case the device already has permission
            Log.i("Permissions", "Camera permission already granted")

            val takePicture = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(takePicture, cameraIntentCode) //code 1 is gallery access
        }
    }
    fun onOpenGallery(view: View) {
        //Ask for storage permissions
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), galleryPermissionCode)
        }
        else { //In case the device already has permission
            Log.i("Permissions", "Galley permission already granted")

            val selectPicture = Intent(Intent.ACTION_PICK)
            selectPicture.type = "image/*"
            startActivityForResult(selectPicture, galleryIntentCode) //code 2 is gallery access
        }
    }

    //Ask for permissions to use or the camera or the gallery
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == cameraPermissionCode) { //CAMERA PERMISSION ACCESS
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i("Permissions", "Camera permission granted")

                //Start activity takePicture
                val takePicture = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(takePicture, cameraIntentCode) //code 1 is gallery access
            }
            else
                Log.i("Permissions", "Camera permission denied")
        }
        else if (requestCode == galleryPermissionCode) { //GALLERY PERMISSION ACCESS
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i("Permissions", "Gallery permission granted")

                //Start activity selectPicture
                val selectPicture = Intent(Intent.ACTION_PICK)
                selectPicture.type = "image/*"
                startActivityForResult(selectPicture, galleryIntentCode) //code 2 is gallery access
            }
            else
                Log.i("Permissions", "Gallery permission denied")
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    //onActivityResult
    private lateinit var filePath : String
    @SuppressLint("Recycle") //In order to suppress the warning at .query
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == cameraIntentCode && resultCode == Activity.RESULT_OK && data != null) { //Camera Access
            if (data.extras == null)
                Toast.makeText(applicationContext, "Error loading image", Toast.LENGTH_LONG).show()

            val bitmap = data.extras?.get("data") as Bitmap
            imageView.setImageBitmap(bitmap) //Set bitmap in imageView
            val btn = findViewById<ImageButton>(R.id.deleteImageBtn)
            btn.visibility = View.VISIBLE //Button visibility true
            ModelView.deleteImageButton = true //Flag to show the button in case of rotation
        }
        else if (requestCode == galleryIntentCode && resultCode == Activity.RESULT_OK && data != null) { //Gallery Access
            //Reference to gallery image
            var uri = data.data?.apply {
                val cursor = contentResolver.query(
                        this,
                        arrayOf(MediaStore.Images.ImageColumns.DATA),
                        null,
                        null,
                        null
                )

                //Get the filepath for the image
                if (cursor != null && cursor.moveToFirst())
                    filePath = cursor.getString(0)

                //Get the bitmap from path
                bitmap = BitmapFactory.decodeFile(filePath)
                imageView.setImageBitmap(bitmap) //Set bitmap in imageView
                val btn = findViewById<ImageButton>(R.id.deleteImageBtn)
                btn.visibility = View.VISIBLE //Button visibility true
                ModelView.deleteImageButton = true //Flag to show the button in case of rotation
            }
            return
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    //Delete picture from product
    fun onDeletePicture(view: View) {
        ModelView.deleteImageButton = false
        val btn = findViewById<ImageButton>(R.id.deleteImageBtn)
        Model.getProdById(prodId, listId)!!.image = null
        btn.visibility = View.INVISIBLE

        imageView.setImageBitmap(null)
        imageView.adjustViewBounds = true
        imageView.invalidate()
    }

    //Methods that will fully or partially fill with information the layout
    private fun fillOptions() {
        val sL = Model.getListById(listId)
        findViewById<EditText>(R.id.edProductName).setText(sL!!.returnProduct(prodId)!!.name)
        findViewById<EditText>(R.id.edBrand).setText(sL.returnProduct(prodId)!!.brand)
        findViewById<EditText>(R.id.edPrice).setText(sL.returnProduct(prodId)!!.price.toString())
        findViewById<EditText>(R.id.edNotes).setText(sL.returnProduct(prodId)!!.notes)
        findViewById<EditText>(R.id.edQuantity).setText(sL.returnProduct(prodId)!!.amount.toString())

        val imageBtn = findViewById<ImageButton>(R.id.deleteImageBtn)
        val img = sL.returnProduct(prodId)!!.image
        findViewById<ImageView>(R.id.productImageView).setImageBitmap(transformIntoBitmap(img))
        if (img != null)
            imageBtn.visibility = View.VISIBLE

        setCategory(sL.returnProduct(prodId)!!.category)
        setUnit(sL.returnProduct(prodId)!!.units)
    }
    private fun fillPartialOpts() {
        findViewById<EditText>(R.id.edProductName).setText(dataName)
        searchCategory(dataCat)
    }

    //Add a new type of categories or new type of units
    fun onNewCategory(view: View) {
        ModelView.dialogNewCategoryShowing = true
        val builder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val vView: View = inflater.inflate(R.layout.dialog_new_category, null) //The layout to inflate
        editText = vView.findViewById(R.id.newCategoryName)

        builder.setView(vView)
        builder.setCancelable(true)
        builder.setOnCancelListener { ModelView.dialogNewCategoryShowing = false }
        builder.setPositiveButton(getString(R.string.add)) { dialog, _ ->
            ModelView.dialogNewCategoryShowing = false
            dialog.dismiss()
            val newCatName = editText!!.text.toString()
            addToCategories(newCatName)
        }
        builder.setNegativeButton(getString(R.string.dialog_back)) { dialog, _ ->
            ModelView.dialogNewCategoryShowing = false
            dialog.dismiss()
        }
        dialogNewCategory = builder.show()
    }
    private fun addToCategories(name: String) {
        if(!Model.config.categories.contains(name)){
            Model.config.categories.add(name)
            if (Model.config.categories.size == 1)
                loadCategories()
        }
    }
    fun onNewUnitType(view: View) {
        ModelView.dialogNewUnitsShowing = true
        val builder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val vView: View = inflater.inflate(R.layout.dialog_new_unit, null) //The layout to inflate
        editText = vView.findViewById(R.id.newUnitName)

        builder.setView(vView)
        builder.setCancelable(true)
        builder.setOnCancelListener { ModelView.dialogNewUnitsShowing = false }
        builder.setPositiveButton(getString(R.string.add)) { dialog, _ ->
            ModelView.dialogNewUnitsShowing = false
            dialog.dismiss()
            val newUnitName = editText!!.text.toString()
            addToUnits(newUnitName)
        }
        builder.setNegativeButton(getString(R.string.dialog_back)) { dialog, _ ->
            ModelView.dialogNewUnitsShowing = false
            dialog.dismiss()
        }
        dialogNewUnit = builder.show()
    }
    private fun addToUnits(name: String) {
        if (!Model.config.units.contains(name)) {
            Model.config.units.add(name)
            if (Model.config.units.size == 1)
                loadUnits()
        }
    }

    //Getters and setters of categories and units
    private fun searchCategory(category: String?){
        if(category == null){
            return
        }
        var counter = 0
        for(i in Model.config.categories){
            if(i == category){
                spCategory.setSelection(counter)
                spCategory.invalidate()
                break
            }
            counter++
        }
    }
    private fun setCategory(category: String){ //Not ideal strings
        var counter = 0
        for(i in Model.config.categories){
            if(i == category){
                spCategory.setSelection(counter)
                spCategory.invalidate()
                break
            }
            counter++
        }
    }
    private fun getCategory(): String { //Not ideal strings
        return try {
            spCategory.selectedItem.toString()
        } catch (e: NullPointerException) {
            ""
        }
    }
    private fun setUnit(unit: String){
        var counter = 0
        for(i in Model.config.units){
            if(i == unit){
                spUnit.setSelection(counter)
                spUnit.invalidate()
                break
            }
            counter++
        }
    }
    private fun getUnit(): String {
        return try {
            spUnit.selectedItem.toString()
        } catch (e: NullPointerException) {
            ""
        }
    }

    //Units and categories loaded
    private fun loadUnits() {
        val arrayAdapter: ArrayAdapter<String> = ArrayAdapter(this, android.R.layout.simple_spinner_item, Model.config.units)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spUnit.adapter = arrayAdapter
    }
    private fun loadCategories() {
        val arrayAdapter: ArrayAdapter<String> = ArrayAdapter(this, android.R.layout.simple_spinner_item, Model.config.categories)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCategory.adapter = arrayAdapter
    }

    //Transform a non serializable object into a serializable byteArray
    private fun transformIntoByteArray(bitmap: Bitmap?): ByteArray? {
        if (bitmap != null) {
            val stream = ByteArrayOutputStream()
            bitmap!!.compress(Bitmap.CompressFormat.PNG, 50, stream)
            return stream.toByteArray()
        }
        return null
    }
    //Transform a serializable byteArray into a Bitmap
    private fun transformIntoBitmap(byteArray: ByteArray?): Bitmap? {
        if (byteArray != null)
            return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        return null
    }
}