import { gemini20Flash, googleAI } from '@genkit-ai/googleai';
import { genkit } from 'genkit';
import { getUserFinancialData } from '@/services/firestore';
import { FinancialGoal } from '@/types/models';

// Khởi tạo Genkit với API key từ environment variable
const ai = genkit({
  plugins: [googleAI({apiKey: "AIzaSyBM3wV8Ol6pSvzsZFr1UpQhuGIPCQjPEIg"})],
  model: gemini20Flash,
});

// Định nghĩa các flows
export const chatFlow = ai.defineFlow('chatFlow', async (message: string) => {
  const prompt = `${message}
  
  Vui lòng sử dụng định dạng markdown trong phản hồi của bạn, bao gồm:
  - Các đề mục với # hoặc ##
  - In đậm với **từ cần in đậm**
  - In nghiêng với *từ cần in nghiêng*
  - Danh sách có thứ tự (1. 2. 3. ...)
  - Danh sách không thứ tự với dấu gạch đầu dòng (-)
  - Bảng khi cần thiết
  
  Trả lời bằng tiếng Việt, chi tiết và dễ hiểu.`;
  
  const { text } = await ai.generate(prompt);
  return text;
});

export const financialAdvisorFlow = ai.defineFlow('financialAdvisorFlow', async (input: { userId: string; message: string }) => {
  // Lấy dữ liệu tài chính của user
  const financialData = await getUserFinancialData(input.userId);
  console.log('Financial Data:', financialData.goals);
  
  // Tạo prompt chi tiết với dữ liệu tài chính
  const prompt = `
Dựa trên dữ liệu tài chính sau của người dùng và câu hỏi của họ, hãy đưa ra lời khuyên và phân tích chi tiết:

## TỔNG QUAN TÀI CHÍNH
- **Tổng thu nhập**: ${financialData.analysis.totalIncome}
- **Tổng chi tiêu**: ${financialData.analysis.totalExpense}
- **Thu nhập ròng**: ${financialData.analysis.netIncome}

## CHI TIÊU THEO DANH MỤC
${Object.entries(financialData.analysis.categoryExpenses)
  .map(([category, amount]) => `- **${category}**: ${amount}`)
  .join('\n')}

## TÌNH TRẠNG NGÂN SÁCH
${financialData.analysis.budgetStatus
  .map((b: { category: string; amount: number; spent: number; percentageUsed: number }) =>
    `- **${b.category}**: Đã dùng ${b.percentageUsed.toFixed(1)}% (${b.spent}/${b.amount})`
  ).join('\n')}

${financialData.goals?.length > 0 ? `
## MỤC TIÊU TÀI CHÍNH
${financialData.goals
  .map((g: FinancialGoal) =>
    `- **${g.name}**: ${g.currentAmount}/${g.targetAmount} (${(g.currentAmount/g.targetAmount*100).toFixed(1)}%)`
  ).join('\n')}
` : ''}

**Câu hỏi của người dùng**: ${input.message}

Hãy đưa ra phân tích ngắn gọn nhưng đầy đủ các ý kiến với các mục sau (sử dụng định dạng markdown để làm nổi bật):

### 1. Phân tích tình hình tài chính hiện tại
### 2. Những điểm cần lưu ý hoặc cải thiện
### 3. Lời khuyên và kế hoạch hành động cụ thể
### 4. Dự báo và gợi ý cho tương lai

Vui lòng đảm bảo phản hồi được định dạng tốt với markdown, bao gồm các **đề mục**, *in nghiêng*, **in đậm**, danh sách có thứ tự, và danh sách không thứ tự.

Trả lời bằng tiếng Việt, chi tiết và dễ hiểu.`;

  const { text } = await ai.generate(prompt);
  return text;
});

export const analyzeExpenseFlow = ai.defineFlow('analyzeExpenseFlow', async (expenseData: string) => {
  const prompt = `Phân tích chi tiêu sau và đưa ra nhận xét, gợi ý:
  
  \`\`\`
  ${expenseData}
  \`\`\`
  
  Hãy đưa ra phân tích chi tiết với các mục sau (sử dụng định dạng markdown để làm nổi bật):
  
  ### 1. Tổng quan về các khoản chi
  ### 2. Các khoản chi bất thường hoặc cao bất thường
  ### 3. Gợi ý để tối ưu chi tiêu
  
  Vui lòng đảm bảo phản hồi được định dạng tốt với markdown, bao gồm các **đề mục**, *in nghiêng*, **in đậm**, danh sách có thứ tự, và danh sách không thứ tự.

  Trả lời bằng tiếng Việt, chi tiết và dễ hiểu.`;
  
  const { text } = await ai.generate(prompt);
  return text;
});