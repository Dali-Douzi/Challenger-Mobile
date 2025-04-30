package com.example.challengermobile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;

public class ScrimAdapter extends RecyclerView.Adapter<ScrimAdapter.ViewHolder> {
    private List<Scrim> scrims;

    public ScrimAdapter(List<Scrim> scrims) {
        this.scrims = scrims;
    }

    public void updateList(List<Scrim> newList) {
        this.scrims = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.scrim_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Scrim s = scrims.get(position);
        holder.teamA.setText(s.getTeamA());
        holder.teamB.setText(s.getTeamB());
        holder.scrimDate.setText(new SimpleDateFormat("MM/dd/yyyy HH:mm").format(s.getScrimDate()));
        holder.status.setText(s.getStatus());
    }

    @Override
    public int getItemCount() {
        return scrims.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView teamA, teamB, scrimDate, status;
        public ViewHolder(View itemView) {
            super(itemView);
            teamA     = itemView.findViewById(R.id.teamA);
            teamB     = itemView.findViewById(R.id.teamB);
            scrimDate = itemView.findViewById(R.id.scrimDate);
            status    = itemView.findViewById(R.id.status);
        }
    }
}