def place_at(self, group, x, y):
    group.to_edge(np.array([x, y, 0]))

def move_relative_to_edge(self, group, x, y):
    self.play(ApplyMethod(group.to_edge, np.array([x, y, 0])))

def move_relative_to_obj(self, group, target, x, y):
    self.play(ApplyMethod(group.next_to, target, np.array([x, y, 0])))

def move_arrow_to_line(self, line_number, pointer, code_block):
    line_object = code_block.get_line_at(line_number)
    self.play(FadeIn(pointer.next_to(line_object, LEFT, MED_SMALL_BUFF)))

