USE recipe_nutrition;
SET NAMES utf8mb4;

-- Xóa dữ liệu công thức cũ (thêm WHERE id > 0 để tránh lỗi Safe Update Mode 1175)
DELETE FROM recipes WHERE id > 0;
ALTER TABLE recipes AUTO_INCREMENT = 1;

-- Thêm tài khoản demo chuẩn mật khẩu "password123"
INSERT IGNORE INTO users (id, full_name, email, password_hash) VALUES
(1, 'Demo User', 'test@example.com', '$2a$10$whuqfsBlvgPjNnJreejXaObELZahM7A4e58rGin5EGrJsZQh61w5i');

-- 10 Món ăn Việt Nam kèm link ảnh thật (Unsplash 200 OK)
INSERT INTO recipes (id, title, description, image_url, prep_time, cook_time, servings, calories, protein_g, carbs_g, fat_g) VALUES
(1, 'Phở bò', 'Phở nước trong ngọt vị xương bò, thịt bò tái mềm, thơm mùi gừng, hành nướng và quế hồi.', 'https://byvn.net/ZAgV', 30, 120, 2, 450, 30.0, 55.0, 12.0),
(2, 'Canh chua cá lóc', 'Canh chua miền Nam với cá lóc, dứa, cà chua và đậu bắp.', 'https://byvn.net/D7K6', 20, 20, 3, 180, 20.0, 12.0, 6.0),
(3, 'Gỏi cuốn tôm thịt', 'Bánh tráng cuốn tôm, thịt luộc, bún và rau sống, ăn kèm nước chấm.', 'https://byvn.net/hP8f', 30, 5, 2, 220, 14.0, 30.0, 5.0),
(4, 'Bún chả Hà Nội', 'Thịt nướng thơm lừng ăn cùng bún, rau sống và nước chấm chua ngọt.', 'https://byvn.net/9LEh', 30, 20, 2, 560, 28.0, 65.0, 20.0),
(5, 'Bánh mì pate', 'Bánh mì giòn với pate gan, chả lụa, dưa leo và đồ chua.', 'https://byvn.net/JCul', 10, 5, 1, 450, 16.0, 50.0, 20.0),
(6, 'Salad rau củ', 'Salad rau củ tươi giòn với sốt dầu ô liu và chanh, ít calo, nhiều chất xơ.', 'https://byvn.net/gbkp', 15, 0, 2, 110, 2.5, 12.0, 7.0),
(7, 'Cá kho tộ', 'Cá kho đậm đà với nước màu, tiêu và hành, ăn với cơm nóng.', 'https://byvn.net/HxLg', 15, 40, 3, 260, 24.0, 8.0, 14.0),
(8, 'Thịt kho tàu', 'Thịt ba chỉ kho trứng với nước dừa, mềm béo, đậm vị, món truyền thống ngày Tết.', 'https://byvn.net/E7wg', 15, 60, 4, 520, 30.0, 10.0, 38.0),
(9, 'Bún bò Huế', 'Bún bò cay nồng vị sả và mắm ruốc, nước dùng đậm đà đặc trưng xứ Huế.', 'https://byvn.net/YZfE', 30, 120, 2, 520, 30.0, 62.0, 15.0),
(10, 'Cơm tấm sườn bì chả', 'Cơm tấm với sườn nướng, bì, chả trứng, mỡ hành và nước mắm chua ngọt.', 'https://byvn.net/67eP', 30, 30, 1, 750, 38.0, 85.0, 28.0);

INSERT INTO ingredients (recipe_id, name, quantity, unit) VALUES
-- 1. Phở bò
(1, 'Xương ống bò', 1000, 'g'),
(1, 'Thịt bò (tái hoặc nạm)', 300, 'g'),
(1, 'Bánh phở', 300, 'g'),
(1, 'Gừng', 1, 'củ'),
(1, 'Hành tím', 3, 'củ'),
(1, 'Hồi, quế, thảo quả, đinh hương', 1, 'muỗng canh'),
(1, 'Hành lá, ngò gai, giá', 100, 'g'),
-- 2. Canh chua cá lóc
(2, 'Cá lóc', 400, 'g'),
(2, 'Dứa (thơm)', 100, 'g'),
(2, 'Cà chua', 2, 'quả'),
(2, 'Đậu bắp', 3, 'quả'),
(2, 'Me chua', 2, 'muỗng canh'),
(2, 'Giá đỗ, rau om, ngò gai', 80, 'g'),
-- 3. Gỏi cuốn tôm thịt
(3, 'Bánh tráng', 8, 'cái'),
(3, 'Tôm luộc', 200, 'g'),
(3, 'Thịt ba chỉ luộc', 150, 'g'),
(3, 'Bún tươi', 150, 'g'),
(3, 'Xà lách, rau thơm, hẹ', 100, 'g'),
-- 4. Bún chả Hà Nội
(4, 'Thịt ba chỉ', 300, 'g'),
(4, 'Thịt nạc vai xay', 200, 'g'),
(4, 'Bún tươi', 300, 'g'),
(4, 'Nước mắm', 3, 'muỗng canh'),
(4, 'Đường', 2, 'muỗng canh'),
(4, 'Rau sống, đu đủ xanh', 150, 'g'),
-- 5. Bánh mì pate
(5, 'Bánh mì', 1, 'ổ'),
(5, 'Pate gan', 40, 'g'),
(5, 'Chả lụa', 50, 'g'),
(5, 'Dưa leo', 0.5, 'quả'),
(5, 'Đồ chua (cà rốt, củ cải)', 40, 'g'),
(5, 'Ngò rí', 10, 'g'),
(5, 'Mayonnaise', 1, 'muỗng cà phê'),
-- 6. Salad rau củ
(6, 'Xà lách', 100, 'g'),
(6, 'Cà chua bi', 6, 'quả'),
(6, 'Dưa leo', 1, 'quả'),
(6, 'Cà rốt', 1, 'củ'),
(6, 'Bắp cải tím', 50, 'g'),
(6, 'Dầu ô liu', 1, 'muỗng canh'),
(6, 'Nước cốt chanh', 1, 'muỗng canh'),
-- 7. Cá kho tộ
(7, 'Cá basa hoặc cá lóc', 500, 'g'),
(7, 'Nước mắm', 2, 'muỗng canh'),
(7, 'Đường', 1, 'muỗng canh'),
(7, 'Hành tím', 3, 'củ'),
(7, 'Tiêu xay', 1, 'muỗng cà phê'),
(7, 'Nước dừa', 200, 'ml'),
-- 8. Thịt kho tàu
(8, 'Thịt ba chỉ', 600, 'g'),
(8, 'Trứng (gà hoặc vịt)', 4, 'quả'),
(8, 'Nước dừa tươi', 400, 'ml'),
(8, 'Nước mắm', 3, 'muỗng canh'),
(8, 'Đường', 2, 'muỗng canh'),
(8, 'Hành tím', 3, 'củ'),
(8, 'Tỏi', 3, 'tép'),
(8, 'Tiêu xay', 1, 'muỗng cà phê'),
-- 9. Bún bò Huế
(9, 'Bắp bò', 400, 'g'),
(9, 'Giò heo', 300, 'g'),
(9, 'Bún sợi to', 400, 'g'),
(9, 'Sả', 5, 'cây'),
(9, 'Mắm ruốc', 1, 'muỗng canh'),
(9, 'Sa tế', 2, 'muỗng canh'),
(9, 'Hành tây, giá, bắp chuối, rau sống', 150, 'g'),
-- 10. Cơm tấm sườn bì chả
(10, 'Cơm tấm (đã nấu)', 250, 'g'),
(10, 'Sườn heo', 150, 'g'),
(10, 'Bì heo', 30, 'g'),
(10, 'Chả trứng', 60, 'g'),
(10, 'Dưa leo, cà chua', 80, 'g'),
(10, 'Nước mắm pha chua ngọt', 3, 'muỗng canh'),
(10, 'Mỡ hành', 1, 'muỗng cà phê');

INSERT INTO instructions (recipe_id, step_number, description) VALUES
-- 1. Phở bò
(1, 1, 'Chần xương bò qua nước sôi 5 phút, rửa sạch để nước dùng trong.'),
(1, 2, 'Nướng gừng và hành tím cho thơm, rang sơ hồi, quế, thảo quả, đinh hương.'),
(1, 3, 'Hầm xương cùng gừng, hành và gia vị khoảng 2 giờ, nêm muối, nước mắm và chút đường phèn.'),
(1, 4, 'Trụng bánh phở, xếp thịt bò thái mỏng lên trên, chan nước dùng sôi, ăn kèm hành lá, ngò gai và giá.'),
-- 2. Canh chua cá lóc
(2, 1, 'Cá làm sạch, cắt khúc, ướp chút muối và tiêu.'),
(2, 2, 'Nấu nước me, cho cà chua và dứa vào đun sôi.'),
(2, 3, 'Cho cá vào nấu chín, thêm đậu bắp, nêm nước mắm và đường.'),
(2, 4, 'Cho giá và rau thơm vào, tắt bếp.'),
-- 3. Gỏi cuốn tôm thịt
(3, 1, 'Luộc tôm và thịt ba chỉ, để nguội rồi thái lát mỏng.'),
(3, 2, 'Nhúng nhanh bánh tráng qua nước, trải ra đĩa.'),
(3, 3, 'Xếp rau, bún, thịt, tôm lên bánh tráng rồi cuộn chặt tay.'),
(3, 4, 'Ăn kèm nước chấm tương hoặc nước mắm chua ngọt.'),
-- 4. Bún chả Hà Nội
(4, 1, 'Ướp thịt ba chỉ và thịt xay với hành, tỏi, nước mắm, đường trong 30 phút.'),
(4, 2, 'Nướng thịt trên than hoặc lò nướng đến khi vàng thơm.'),
(4, 3, 'Pha nước chấm gồm nước mắm, đường, chanh, tỏi ớt và nước ấm; cho đu đủ xanh vào ngâm.'),
(4, 4, 'Thả thịt vào bát nước chấm, ăn cùng bún và rau sống.'),
-- 5. Bánh mì pate
(5, 1, 'Nướng bánh mì cho giòn, bổ dọc ổ bánh.'),
(5, 2, 'Phết pate và mayonnaise đều hai mặt bánh.'),
(5, 3, 'Xếp chả lụa, dưa leo, đồ chua và ngò rí vào bánh.'),
(5, 4, 'Thêm ớt hoặc tương ớt tùy khẩu vị rồi thưởng thức ngay.'),
-- 6. Salad rau củ
(6, 1, 'Rửa sạch rau, để ráo nước.'),
(6, 2, 'Cắt xà lách, thái lát dưa leo, bào sợi cà rốt và bắp cải tím, cà chua bi cắt đôi.'),
(6, 3, 'Trộn đều rau củ trong tô lớn.'),
(6, 4, 'Rưới dầu ô liu và nước cốt chanh, nêm muối tiêu, trộn nhẹ rồi dùng ngay.'),
-- 7. Cá kho tộ
(7, 1, 'Ướp cá với nước mắm, đường, hành tím và tiêu trong 20 phút.'),
(7, 2, 'Làm nước màu từ đường, cho vào tộ cùng cá.'),
(7, 3, 'Thêm nước dừa, kho liu riu 30-40 phút đến khi nước sánh lại.'),
-- 8. Thịt kho tàu
(8, 1, 'Thái thịt ba chỉ thành miếng vuông, trụng sơ nước sôi rồi để ráo.'),
(8, 2, 'Luộc trứng chín, bóc vỏ.'),
(8, 3, 'Làm nước màu từ đường, cho thịt vào đảo cho săn lại, thêm hành tỏi băm.'),
(8, 4, 'Đổ nước dừa và nước mắm vào, đun sôi rồi hạ lửa nhỏ, kho 40 phút.'),
(8, 5, 'Cho trứng vào kho thêm 15-20 phút đến khi nước sánh, rắc tiêu.'),
-- 9. Bún bò Huế
(9, 1, 'Chần bắp bò và giò heo, rửa sạch rồi hầm với sả đập dập khoảng 1,5-2 giờ.'),
(9, 2, 'Phi thơm sả băm và sa tế, hòa mắm ruốc với nước rồi lọc, cho vào nồi nước dùng.'),
(9, 3, 'Nêm nếm vừa ăn, vớt thịt ra thái lát mỏng.'),
(9, 4, 'Trụng bún, cho thịt vào tô, chan nước dùng nóng, ăn kèm giá, bắp chuối và rau sống.'),
-- 10. Cơm tấm sườn bì chả
(10, 1, 'Ướp sườn với tỏi, đường, nước mắm, dầu hào trong 30 phút rồi nướng chín vàng.'),
(10, 2, 'Trộn bì heo thái sợi với thính gạo.'),
(10, 3, 'Chuẩn bị chả trứng hấp hoặc nướng, thái miếng.'),
(10, 4, 'Xới cơm tấm ra đĩa, xếp sườn, bì, chả, dưa leo, cà chua, rưới mỡ hành và nước mắm chua ngọt.');
