def place_at(self, group, x, y):
    group.to_edge(np.array([x, y, 0]))

def move_relative_to_edge(self, group, x, y):
    self.play_animation(ApplyMethod(group.to_edge, np.array([x, y, 0])))

def move_relative_to_obj(self, group, target, x, y):
    self.play_animation(ApplyMethod(group.next_to, target, np.array([x, y, 0])))


def place_relative_to_obj(self, group, target, x, y):
    group.next_to(target, np.array([x, y, 0]))

def fade_out_if_needed(self, mobject):
    if mobject in self.mobjects:
        return FadeOut(mobject)
    else:
        return None

def play_animation(self, *args, run_time=1.0):
    time_elapsed = round(self.get_time())
    for time_object in self.time_objects:
        if time_object.end_time <= time_elapsed:
            self.play(time_object.action())
#             self.time_objects.remove(time_object)
    self.play(*args, runtime=run_time)

def move_arrow_to_line(self, line_number, pointer, code_block, code_text):
    idx = 0
    for i in range(line_number):
        idx += len(code_block.code[i])

    if idx > self.code_end:
        animation = self.fade_out_if_needed(pointer)
        if animation is not None:
            self.play(animation, runtime=0.1)
        self.scroll_down(code_text, (idx - self.code_end))
    elif idx - 1 < self.code_start:
        animation = self.fade_out_if_needed(pointer)
        if animation is not None:
            self.play(animation, runtime=0.1)
        self.scroll_up(code_text, (self.code_start - idx+len(code_block.code[line_number-1])))

    line_object = code_block.get_line_at(line_number)
    self.play(FadeIn(pointer.next_to(line_object, LEFT, MED_SMALL_BUFF)))

def scroll_down(self, group, scrolls):
    sh_val = group[self.code_start].get_corner(UP + LEFT)[1] - group[self.code_start + 1].get_corner(UP + LEFT)[1]
    for i in range(1, 1 + scrolls):
        group[self.code_end + i - 1].next_to(group[self.code_end - 2 + i], DOWN, aligned_edge=LEFT)
        self.play(FadeOut(group[self.code_start + i - 1]), FadeIn(group[self.code_end + i - 1]),
                  group[(self.code_start + i):(self.code_end + i)].shift, sh_val * UP, run_time=0.1)
    self.code_start = self.code_start + scrolls
    self.code_end = self.code_end + scrolls

def scroll_up(self, group, scrolls):
    sh_val = group[self.code_start].get_corner(UP + LEFT)[1] - group[self.code_start + 1].get_corner(UP + LEFT)[1]
    for i in range(1, 1 + scrolls):
        group[self.code_start - i].next_to(group[self.code_start - i + 1], UP, aligned_edge=LEFT)
        self.play_animation(FadeOut(group[self.code_end - i]), FadeIn(group[self.code_start - i]),
                            group[(self.code_start - i):(self.code_end - i)].shift, sh_val * DOWN, run_time=0.1)
    self.code_start = self.code_start - scrolls
    self.code_end = self.code_end - scrolls