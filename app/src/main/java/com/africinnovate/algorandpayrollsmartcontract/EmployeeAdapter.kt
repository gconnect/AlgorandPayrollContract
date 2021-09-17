import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.africinnovate.algorandpayrollsmartcontract.Employee
import com.africinnovate.algorandpayrollsmartcontract.R
import com.bumptech.glide.Glide
import timber.log.Timber

class EmployeeAdapter internal constructor(
    private val context: Context
) : RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var employees = emptyList<Employee>()

    var onItemClick: ((Int) -> Unit)? = null


    inner class EmployeeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val name: TextView = itemView.findViewById(R.id.name)
        val profileImage: ImageView = itemView.findViewById(R.id.profile_image)
        val role: TextView = itemView.findViewById(R.id.role)
        val salary: TextView = itemView.findViewById(R.id.salary)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmployeeViewHolder {
        val itemView = inflater.inflate(R.layout.employee_list_item, parent, false)
        return EmployeeViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: EmployeeViewHolder, position: Int) {
        val current = employees[position]
        holder.name.text = current.name
        Glide.with(context)
            .load(current.profilePics).into(holder.profileImage)
        holder.role.text = current.role
        holder.salary.text = current.salary.toString()


        holder.itemView.setOnClickListener {
            onItemClick?.invoke(position)
        }

    }

    internal fun setData(employees: List<Employee>) {
        this.employees = employees
        notifyDataSetChanged()
    }

    override fun getItemCount() = employees.size

}