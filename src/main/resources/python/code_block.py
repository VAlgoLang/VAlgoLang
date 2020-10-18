class Code_block:
    def __init__(self, code, color=WHITE):
        group = VGroup()
        for c in code:
            group.add(TextMobject(c, color=color))
        self.group = group

    def build(self):
        return self.group.arrange(DOWN, aligned_edge=LEFT)

    def get_line_at(self, line_number):
        return self.group[line_number - 1]
