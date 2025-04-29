package com.example.challengermobile;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;

public class ScrimAdapter extends RecyclerView.Adapter<ScrimAdapter.ScrimViewHolder> {

    private final List<Scrim> scrimList;

    public ScrimAdapter(List<Scrim> scrimList) {
        this.scrimList = scrimList;
    }

    @NonNull
    @Override
    public ScrimViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.scrim_item, parent, false);
        return new ScrimViewHolder(itemView);
    }

    @SuppressLint("SimpleDateFormat")
    @Override
    public void onBindViewHolder(ScrimViewHolder holder, int position) {
        Scrim scrim = scrimList.get(position);
        holder.teamA.setText(scrim.getTeamA());
        holder.teamB.setText(scrim.getTeamB());
        holder.scrimDate.setText(new SimpleDateFormat("MM/dd/yyyy").format(scrim.getScrimDate()));
        holder.status.setText(scrim.getStatus());
    }

    @Override
    public int getItemCount() {
        return scrimList.size();
    }

    public static class ScrimViewHolder extends RecyclerView.ViewHolder {
        public TextView teamA, teamB, scrimDate, status;

        public ScrimViewHolder(View view) {
            super(view);
            teamA = view.findViewById(R.id.teamA);
            teamB = view.findViewById(R.id.teamB);
            scrimDate = view.findViewById(R.id.scrimDate);
            status = view.findViewById(R.id.status);
        }
    }
}
