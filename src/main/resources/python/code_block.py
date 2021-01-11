class CodeBlock:
    def __init__(self, code, boundaries, syntax_highlighting=True, syntax_highlighting_style="inkpot", text_color=WHITE,
                 text_weight=NORMAL, font="Times New Roman", tab_spacing=2):
        group = VGroup()
        self.boundaries = boundaries

        self.move_position = np.array(
            [(boundaries[0][0] + boundaries[1][0]) / 2 + SMALL_BUFF, (boundaries[0][1] + boundaries[3][1]) / 2, 0])
        self.boundary_width = boundaries[1][0] - boundaries[0][0]
        arrow_size = self.boundary_width * 0.7 / 5.0
        self.boundary_width -= arrow_size
        self.boundary_height = boundaries[0][1] - boundaries[3][1]
        self.code_end = max(math.floor(self.boundary_height * 12.0 / self.boundary_width), 2)

        if syntax_highlighting:
            fp = tempfile.NamedTemporaryFile(suffix='.re')

            for c in code:
                for sc in c:
                    fp.write(bytes(sc + "\n", encoding='utf-8'))

            fp.seek(0)

            self.paragraph = Code(fp.name, style=syntax_highlighting_style, language="reasonml",
                                  tab_width=tab_spacing).code
            fp.close()
            group.add(self.paragraph)
            self.all = self.paragraph
        else:
            for c in code:
                for sc in c:
                    text = Text(sc, color=text_color, weight=text_weight, font=font)
                    group.add(text)
            self.all = group
        self.code = code

    def build(self):
        self.all.arrange_submobjects(DOWN * 0.1, aligned_edge=LEFT)
        ratio = 4.6 / 5.0
        self.all.set_width(self.boundary_width * ratio)
        return self.all

    def get_line_at(self, line_number):
        idx = 0
        for i in range(line_number):
            idx += len(self.code[i])
        return self.all[idx - 1]
