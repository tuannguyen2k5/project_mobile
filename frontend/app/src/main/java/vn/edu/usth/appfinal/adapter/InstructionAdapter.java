package vn.edu.usth.appfinal.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import vn.edu.usth.appfinal.R;
import vn.edu.usth.appfinal.model.Instruction;

import java.util.List;

public class InstructionAdapter extends RecyclerView.Adapter<InstructionAdapter.ViewHolder> {

    private final List<Instruction> instructions;

    public InstructionAdapter(List<Instruction> instructions) {
        this.instructions = instructions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_instruction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Instruction item = instructions.get(position);
        holder.tvStepNumber.setText(String.valueOf(item.getStepNumber()));
        String desc = item.getDescription();
        holder.tvStepDescription.setText((desc != null && !desc.equalsIgnoreCase("null")) ? desc : "");
    }

    @Override
    public int getItemCount() {
        return instructions != null ? instructions.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStepNumber;
        TextView tvStepDescription;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStepNumber = itemView.findViewById(R.id.tvStepNumber);
            tvStepDescription = itemView.findViewById(R.id.tvStepDescription);
        }
    }
}
