import { NextResponse } from 'next/server';
import { chatFlow, analyzeExpenseFlow, financialAdvisorFlow } from '@/config/genkit';

export async function POST(request: Request) {
  try {
    const { type, content } = await request.json();

    switch (type) {
      case 'chat':
        const chatResponse = await chatFlow(content);
        return NextResponse.json({ response: chatResponse });

      case 'financial-advice':
        if (!content.userId || !content.message) {
          return NextResponse.json(
            { error: 'Thiếu userId hoặc message' },
            { status: 400 }
          );
        }
        const adviceResponse = await financialAdvisorFlow({
          userId: content.userId,
          message: content.message
        });
        return NextResponse.json({ response: adviceResponse });

      case 'analyze':
        const analysisResponse = await analyzeExpenseFlow(content);
        return NextResponse.json({ response: analysisResponse });

      default:
        return NextResponse.json(
          { error: 'Loại yêu cầu không hợp lệ' },
          { status: 400 }
        );
    }
  } catch (error) {
    console.error('Lỗi khi xử lý yêu cầu AI:', error);
    return NextResponse.json(
      { error: 'Có lỗi xảy ra khi xử lý yêu cầu' },
      { status: 500 }
    );
  }
}