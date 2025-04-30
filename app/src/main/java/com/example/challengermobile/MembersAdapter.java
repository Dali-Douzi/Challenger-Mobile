package com.example.challengermobile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MembersAdapter extends RecyclerView.Adapter<MembersAdapter.MemberViewHolder> {

    private List<Member> memberList;
    private boolean isOwner;

    /**
     * @param memberList the list of members (id, name, role)
     * @param isOwner    true if the current user is the team owner
     */
    public MembersAdapter(List<Member> memberList, boolean isOwner) {
        this.memberList = memberList;
        this.isOwner = isOwner;
    }

    /** Replace the list of members and refresh */
    public void updateList(List<Member> newList) {
        this.memberList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View item = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member, parent, false);
        return new MemberViewHolder(item);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        Member m = memberList.get(position);
        holder.tvName.setText(m.getName());
        holder.tvRole.setText(m.getRole());

        if (isOwner) {
            // TODO: show UI to change this member’s role or remove them
            // e.g. holder.itemView.setOnClickListener(...)
        }
    }

    @Override
    public int getItemCount() {
        return memberList.size();
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvRole;

        MemberViewHolder(View view) {
            super(view);
            tvName = view.findViewById(R.id.tvMemberName);
            tvRole = view.findViewById(R.id.tvMemberRole);
        }
    }
}
