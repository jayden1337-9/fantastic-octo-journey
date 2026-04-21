package com.discord.bot;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ServerAdapter extends RecyclerView.Adapter<ServerAdapter.ViewHolder> {

    private List<ServerItem> items;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(ServerItem item);
    }

    public ServerAdapter(List<ServerItem> items) {
        this.items = items;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_server, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        ServerItem item = items.get(position);

        holder.nameView.setText(item.name);

        if (item.name != null && item.name.length() > 0) {
            holder.iconView.setText(
                    String.valueOf(item.name.charAt(0)).toUpperCase()
            );
        } else {
            holder.iconView.setText("?");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView iconView;
        TextView nameView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            iconView = itemView.findViewById(R.id.iconView);
            nameView = itemView.findViewById(R.id.nameView);
        }
    }
}
