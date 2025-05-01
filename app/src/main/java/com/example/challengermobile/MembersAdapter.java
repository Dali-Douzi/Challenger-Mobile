package com.example.challengermobile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MembersAdapter extends RecyclerView.Adapter<MembersAdapter.MemberViewHolder> {

    public interface RoleChangeListener {
        void onRoleChanged(String memberId, String newRole);
    }

    public List<Member> getMemberList() {
        return memberList;
    }
    private List<Member> memberList;
    private boolean isOwner;
    private RoleChangeListener roleChangeListener;

    public MembersAdapter(List<Member> memberList, boolean isOwner, RoleChangeListener roleChangeListener) {
        this.memberList = memberList;
        this.isOwner = isOwner;
        this.roleChangeListener = roleChangeListener;
    }

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
            holder.tvRole.setVisibility(View.GONE);
            holder.spinnerRole.setVisibility(View.VISIBLE);

            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                    holder.itemView.getContext(),
                    R.array.team_role_list,
                    android.R.layout.simple_spinner_item
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            holder.spinnerRole.setAdapter(adapter);

            int spinnerPosition = adapter.getPosition(m.getRole());
            holder.spinnerRole.setSelection(spinnerPosition);

            holder.spinnerRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                    String newRole = parent.getItemAtPosition(pos).toString();
                    if (!newRole.equals(m.getRole())) {
                        m.setRole(newRole);
                        roleChangeListener.onRoleChanged(m.getId(), newRole);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) { }
            });
        } else {
            holder.tvRole.setVisibility(View.VISIBLE);
            holder.spinnerRole.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return memberList.size();
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvRole;
        Spinner spinnerRole;

        MemberViewHolder(View view) {
            super(view);
            tvName = view.findViewById(R.id.tvMemberName);
            tvRole = view.findViewById(R.id.tvMemberRole);
            spinnerRole = view.findViewById(R.id.spinnerMemberRole);
        }
    }
}