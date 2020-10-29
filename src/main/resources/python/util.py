def place_at(self, group, x, y):
    group.to_edge(np.array([x, y, 0]))

def move_relative_to_edge(self, group, x, y):
    self.play(ApplyMethod(group.to_edge, np.array([x, y, 0])))

def move_relative_to_obj(self, group, target, x, y):
    self.play(ApplyMethod(group.next_to, target, np.array([x, y, 0])))

def place_relative_to_obj(self, group, target, x, y):
    group.next_to(target, np.array([x, y, 0]))

def move_arrow_to_line(self, line_number, pointer, code_block, code_text):
    if line_number > self.code_end:
        self.play(FadeOut(pointer), runtime=0.1)
        # [["test, test1"], ["test2", "test3"]]
        self.scroll_down(code_text, (line_number - self.code_end), self.code_end - self.code_start)
    elif line_number - 1 < self.code_start:
        self.play(FadeOut(pointer), runtime=0.1)
        self.scroll_up(code_text, (self.code_start - line_number + 1), self.code_end - self.code_start)
    line_object = code_block.get_line_at(line_number)
    self.play(FadeIn(pointer.next_to(line_object, LEFT, MED_SMALL_BUFF)))

def scroll_down(self, group, scrolls, visible_elements):
    sh_val = group[self.code_start].get_corner(UP + LEFT)[1] - group[self.code_start + 1].get_corner(UP + LEFT)[1]
    for i in range(1, 1 + scrolls):
        group[self.code_end + i - 1].next_to(group[self.code_end - 2 + i], DOWN, aligned_edge=LEFT)
        self.play(FadeOut(group[self.code_start + i - 1]), FadeIn(group[self.code_end + i - 1]),
                  group[(self.code_start + i):(self.code_end + i)].shift, sh_val * UP, run_time=0.1)
    self.code_start = self.code_start + scrolls
    self.code_end = self.code_end + scrolls

def scroll_up(self, group, scrolls, visible_elements):
    sh_val = group[self.code_start].get_corner(UP + LEFT)[1] - group[self.code_start + 1].get_corner(UP + LEFT)[1]
    for i in range(1, 1 + scrolls):
        group[self.code_start - i].next_to(group[self.code_start - i + 1], UP, aligned_edge=LEFT)
        self.play(FadeOut(group[self.code_end - i]), FadeIn(group[self.code_start - i]),
                  group[(self.code_start - i):(self.code_end - i)].shift, sh_val * DOWN, run_time=0.1)
    self.code_start = self.code_start - scrolls
    self.code_end = self.code_end - scrolls