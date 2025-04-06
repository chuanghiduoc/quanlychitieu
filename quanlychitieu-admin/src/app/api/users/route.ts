import { NextResponse } from 'next/server';
import { firebaseAdmin } from '@/config/firebase';
import { UserResponse, UsersApiResponse, ApiError } from '@/types/api';

export async function GET() {
  try {
    const listUsers = await firebaseAdmin.auth().listUsers();
    const users: UserResponse[] = listUsers.users.map(user => ({
      uid: user.uid,
      email: user.email || null,
      displayName: user.displayName || null,
      disabled: user.disabled || false
    }));
    
    const response: UsersApiResponse = { users };
    return NextResponse.json(response);
  } catch (error) {
    console.error('Lỗi khi lấy danh sách users:', error);
    const errorResponse: ApiError = { error: 'Có lỗi xảy ra khi lấy danh sách users' };
    return NextResponse.json(
      errorResponse,
      { status: 500 }
    );
  }
}

export async function PATCH(request: Request) {
  try {
    const { uid, disabled } = await request.json();
    
    if (!uid) {
      return NextResponse.json(
        { error: 'Thiếu uid của user' },
        { status: 400 }
      );
    }

    await firebaseAdmin.auth().updateUser(uid, { disabled });
    
    return NextResponse.json({ success: true });
  } catch (error) {
    console.error('Lỗi khi cập nhật trạng thái user:', error);
    return NextResponse.json(
      { error: 'Có lỗi xảy ra khi cập nhật trạng thái user' },
      { status: 500 }
    );
  }
}